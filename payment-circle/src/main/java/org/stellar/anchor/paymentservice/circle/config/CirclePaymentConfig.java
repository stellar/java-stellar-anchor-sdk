package org.stellar.anchor.paymentservice.circle.config;

public interface CirclePaymentConfig {
  String getName();

  boolean isEnabled();

  String getUrl();

  String getSecretKey();

  String getHorizonUrl();

  String getNetwork();
}
