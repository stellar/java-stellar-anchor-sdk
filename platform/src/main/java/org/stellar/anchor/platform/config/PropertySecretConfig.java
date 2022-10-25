package org.stellar.anchor.platform.config;

import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.platform.configurator.SecretManager;

public class PropertySecretConfig implements SecretConfig {
  public String getSep10JwtSecretKey() {
    return SecretManager.getInstance().get("secret.sep10.jwt_secret");
  }

  public String getSep10SigningSeed() {
    return SecretManager.getInstance().get("secret.sep10.signing_seed");
  }

  public String getCallbackApiSecret() {
    return SecretManager.getInstance().get("secret.callback_api.auth_secret");
  }

  public String getPlatformApiSecret() {
    return SecretManager.getInstance().get("secret.platform_api.auth_secret");
  }
}
