package howto.oauth_oidc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

public class MainVerticle extends AbstractVerticle {

  private static final String CLIENT_ID =
    System.getenv("GITHUB_CLIENT_ID");
  private static final String CLIENT_SECRET =
    System.getenv("GITHUB_CLIENT_SECRET");  // <1>

  @Override
  public void start(Promise<Void> startPromise) {

    HandlebarsTemplateEngine engine =
      HandlebarsTemplateEngine.create(vertx);     // <2>

    Router router = Router.router(vertx);         // <3>

    router.get("/")                               // <4>
      .handler(ctx -> {
        // we pass the client id to the template
        ctx.put("client_id", CLIENT_ID);
        // and now delegate to the engine to render it.
        engine.render(ctx.data(), "views/index.hbs")
          .onSuccess(buffer -> {
            ctx.response()
              .putHeader("Content-Type", "text/html")
              .end(buffer);
          })
          .onFailure(ctx::fail);
      });

    OAuth2Auth authProvider = GithubAuth.create(vertx, CLIENT_ID, CLIENT_SECRET);

    router.get("/protected")                      // <5>
      .handler(
        OAuth2AuthHandler.create(vertx, authProvider)   // <6>
          .setupCallback(router.route("/callback"))
          .withScope("user:email"))               // <7>
      .handler(ctx -> {
        ctx.response()
          .end("Hello protected!");
      });

    vertx.createHttpServer()                      // <8>
      .requestHandler(router)
      .listen(Integer.getInteger("port", 8080))
      .onSuccess(server -> {
        System.out.println(
          "HTTP server started on port: " + server.actualPort());
        startPromise.complete();
      }).onFailure(startPromise::fail);
  }
}
