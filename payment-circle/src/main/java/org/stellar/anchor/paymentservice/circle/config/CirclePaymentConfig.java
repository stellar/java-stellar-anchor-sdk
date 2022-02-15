package org.stellar.anchor.paymentservice.circle.config;

public interface CirclePaymentConfig {
  String getName();

  boolean isEnabled();

  String getCircleUrl();

  String getSecretKey();

  String getHorizonUrl();

  String getStellarNetwork();
}
