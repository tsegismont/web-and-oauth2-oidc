package howto.oauth_oidc;

import io.vertx.core.Handler;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

class ProtectedProfileHandler implements Handler<RoutingContext> {

  private final OAuth2Auth authProvider;
  private final HandlebarsTemplateEngine engine;

  ProtectedProfileHandler(OAuth2Auth authProvider, HandlebarsTemplateEngine engine) {
    this.authProvider = authProvider;
    this.engine = engine;
  }

  @Override
  public void handle(RoutingContext ctx) {
    authProvider
      .userInfo(ctx.user())       // <1>
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
  }
}
