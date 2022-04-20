package org.stellar.anchor.config;

public interface CirclePaymentObserverConfig {
  boolean isEnabled();

  String getCircleUrl();

  String getSecretKey();
}
