package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.IntegrationAuthConfig.AuthType;

@Data
@Configuration
public class IntegrationAuthSettings {
  AuthType authType = AuthType.NONE;
  String platformToAnchorSecret;
  String anchorToPlatformSecret;
  long expirationMilliseconds = 30000;
}
