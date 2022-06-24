package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.IntegrationAuthConfig;

@Data
public class PropertyIntegrationAuthConfig implements IntegrationAuthConfig {
  String platformToAnchorJwtSecret;
  String anchorToPlatformJwtSecret;
  long expirationMilliseconds = 10000;
}
