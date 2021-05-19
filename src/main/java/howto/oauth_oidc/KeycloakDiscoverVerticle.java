package howto.oauth_oidc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;

public class KeycloakDiscoverVerticle extends AbstractVerticle {

  private static final String CLIENT_ID =
    System.getenv("GITHUB_CLIENT_ID");
  private static final String CLIENT_SECRET =
    System.getenv("GITHUB_CLIENT_SECRET");

  @Override
  public void start(Promise<Void> startPromise) {
    OAuth2Options options = new OAuth2Options()
      .setClientID(CLIENT_ID)
      .setClientSecret(CLIENT_SECRET)
      .setTenant("vertx-test")          // <1>
      .setSite("https://your.keycloak.instance/auth/realms/{tenant}"); // <2>

    KeycloakAuth
      .discover(vertx, options)
      .onFailure(startPromise::fail)
      .onSuccess(authProvider -> {
        // use the authProvider like before to
        // protect your application
      });
  }
}
