package org.stellar.anchor.config;

public interface CirclePaymentObserverConfig {
  boolean isEnabled();

  /** @return "TESTNET" or "PUBLIC". */
  String getStellarNetwork();

  String getHorizonUrl();

  String getTrackedWallet();
}
