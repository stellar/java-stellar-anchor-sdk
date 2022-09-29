package org.stellar.anchor.config;

public interface SecretConfig {
  String getSep10JwtSecretKey();

  String getSep10SigningSeed();

  String getCallbackApiSecret();

  String getPlatformApiSecret();

  String getCircleApiKey();
}