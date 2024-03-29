package org.stellar.anchor.config;

import javax.crypto.SecretKey;

public interface CustodySecretConfig {
  String getFireblocksApiKey();

  String getFireblocksSecretKey();

  String getCustodyAuthSecret();

  SecretKey getCustodyAuthSecretKey();
}
