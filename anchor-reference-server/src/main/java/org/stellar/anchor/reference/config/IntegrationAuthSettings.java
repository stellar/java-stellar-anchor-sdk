package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.AuthType;

@Data
@Configuration
@ConfigurationProperties(prefix = "integration-auth")
public class IntegrationAuthSettings {
  AuthType authType = AuthType.NONE;
  String platformToAnchorSecret;
  String anchorToPlatformSecret;
  Long expirationMilliseconds;
}
