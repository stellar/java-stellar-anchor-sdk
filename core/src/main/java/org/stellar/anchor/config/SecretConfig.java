package org.stellar.anchor.config;

import javax.crypto.SecretKey;

public interface SecretConfig {
  SecretKey getSep6MoreInfoUrlJwtSecret();

  SecretKey getSep10JwtSecretKey();

  String getSep10SigningSeed();

  SecretKey getSep24InteractiveUrlJwtSecret();

  SecretKey getSep24MoreInfoUrlJwtSecret();

  String getCallbackAuthSecret();

  SecretKey getCallbackAuthSecretKey();

  String getPlatformAuthSecret();

  SecretKey getPlatformAuthSecretKey();

  String getDataSourceUsername();

  String getDataSourcePassword();

  String getEventsQueueKafkaUsername();

  String getEventsQueueKafkaPassword();
}
