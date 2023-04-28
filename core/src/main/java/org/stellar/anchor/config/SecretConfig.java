package org.stellar.anchor.config;

public interface SecretConfig {
  String getSep10JwtSecretKey();

  String getSep10SigningSeed();

  String getSep24InteractiveUrlJwtSecret();

  String getSep24MoreInfoUrlJwtSecret();

  String getCallbackApiSecret();

  String getPlatformApiSecret();

  String getCustodyApiSecret();

  String getDataSourceUsername();

  String getDataSourcePassword();

  String getFireblocksApiKey();

  String getFireblocksSecretKey();
}
