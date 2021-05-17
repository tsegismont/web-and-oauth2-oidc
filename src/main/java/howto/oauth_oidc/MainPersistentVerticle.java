package howto.oauth_oidc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

public class MainPersistentVerticle extends AbstractVerticle {

  private static final String CLIENT_ID =
    System.getenv("GITHUB_CLIENT_ID");
  private static final String CLIENT_SECRET =
    System.getenv("GITHUB_CLIENT_SECRET");

  // tag::persistent[]
  @Override
  public void start(Promise<Void> startPromise) {

    HandlebarsTemplateEngine engine =
      HandlebarsTemplateEngine.create(vertx);

    Router router = Router.router(vertx);

    router.route()
      .handler(SessionHandler
        .create(LocalSessionStore.create(vertx)));  // <1>

    router.get("/")
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

    // ...
    // end::persistent[]

    OAuth2Auth authProvider = GithubAuth.create(vertx, CLIENT_ID, CLIENT_SECRET);

    router.get("/protected")
      .handler(
        OAuth2AuthHandler.create(vertx, authProvider)
          .setupCallback(router.route("/callback"))
          .withScope("user:email"))
      .handler(ctx -> {
        authProvider
          .userInfo(ctx.user())
          .onFailure(err -> {
            ctx.session().destroy();
            ctx.fail(err);
          })
          .onSuccess(userInfo -> {
            // fetch the user emails from the github API
            WebClient.create(ctx.vertx())
              .getAbs("https://api.github.com/user/emails")
              .authentication(new TokenCredentials(ctx.user().<String>get("access_token"))) // <2>
              .as(BodyCodec.jsonArray())
              .send()
              .onFailure(err -> {
                ctx.session().destroy();
                ctx.fail(err);
              })
              .onSuccess(res -> {
                userInfo.put("private_emails", res.body());
                // we pass the client info to the template
                ctx.put("userInfo", userInfo);
                // and now delegate to the engine to render it.
                engine.render(ctx.data(), "views/protected.hbs")
                  .onSuccess(buffer -> {
                    ctx.response()
                      .putHeader("Content-Type", "text/html")
                      .end(buffer);
                  })
                  .onFailure(ctx::fail);
              });
          });
      });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(Integer.getInteger("port", 8080))
      .onSuccess(server -> {
        System.out.println(
          "HTTP server started on port: " + server.actualPort());
        startPromise.complete();
      }).onFailure(startPromise::fail);
  }
}
