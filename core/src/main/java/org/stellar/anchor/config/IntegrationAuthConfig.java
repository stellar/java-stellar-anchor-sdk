package org.stellar.anchor.config;

public interface IntegrationAuthConfig {
  AuthType getAuthType();

  @Secret
  String getPlatformToAnchorSecret();

  @Secret
  String getAnchorToPlatformSecret();

  long getExpirationMilliseconds();

  enum AuthType {
    NONE,
    API_KEY,
    JWT_TOKEN
  }
}
