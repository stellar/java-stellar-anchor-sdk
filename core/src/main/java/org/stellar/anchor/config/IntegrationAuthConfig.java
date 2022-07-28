package org.stellar.anchor.config;

public interface IntegrationAuthConfig {
  AuthType getAuthType();

  @Secret
  String getPlatformToAnchorSecret();

  @Secret
  String getAnchorToPlatformSecret();

  Long getExpirationMilliseconds();

  enum AuthType {
    NONE,
    API_KEY,
    JWT_TOKEN
  }
}
