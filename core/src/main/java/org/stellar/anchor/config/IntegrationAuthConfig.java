package org.stellar.anchor.config;

public interface IntegrationAuthConfig {
  @Secret
  String getPlatformToAnchorJwtSecret();

  @Secret
  String getAnchorToPlatformJwtSecret();

  long getExpirationMilliseconds();
}
