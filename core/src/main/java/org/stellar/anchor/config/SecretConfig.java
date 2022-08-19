package org.stellar.anchor.config;

public interface SecretConfig {
  String getJwtSecretKey();

  String getSep10SigningSeed();
}
