package org.stellar.anchor.config;

public interface PaymentObserverConfig {
  boolean isEnabled();

  String getTrackedWallet();

  String getCircleUrl();
}
