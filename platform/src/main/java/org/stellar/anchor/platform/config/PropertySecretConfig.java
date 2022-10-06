package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.stellar.anchor.config.SecretConfig;

@Data
public class PropertySecretConfig implements SecretConfig {
  @Value("${secret.sep10.jwt_secret:#{null}}")
  private String sep10JwtSecretKey;

  @Value("${secret.sep10.signing_seed:#{null}}")
  private String sep10SigningSeed;

  @Value("${secret.callback_api.auth_secret:#{null}}")
  private String callbackApiSecret = null;

  @Value("${secret.platform_api.auth_secret:#{null}}")
  private String platformApiSecret = null;

  @Value("${secret.data.username:#{null}}")
  private String dataUsername = null;

  @Value("${secret.data.password:#{null}}")
  private String dataPassword = null;
}
