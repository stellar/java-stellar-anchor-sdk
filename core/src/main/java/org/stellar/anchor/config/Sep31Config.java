package org.stellar.anchor.config;

public interface Sep31Config {
  boolean isEnabled();

  String getFeeIntegrationEndPoint();

  PaymentType getPaymentType();

  enum PaymentType {
    STRICT_SEND,
    STRICT_RECEIVE
  }
}
