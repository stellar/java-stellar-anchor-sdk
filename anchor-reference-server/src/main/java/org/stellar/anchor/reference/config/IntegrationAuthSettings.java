package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "integration-auth")
public class IntegrationAuthSettings {
  String platformToAnchorJwtSecret;
  String anchorToPlatformJwtSecret;
  long expirationMilliseconds = 30000;
}
