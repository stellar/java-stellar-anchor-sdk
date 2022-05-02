package org.stellar.anchor.config;

public interface Sep31Config {
  boolean isEnabled();

  String getFeeIntegrationEndPoint();

  PaymentType getPaymentType();

  MemoGenerator getMemoGenerator();

  enum PaymentType {
    STRICT_SEND,
    STRICT_RECEIVE
  }

  enum MemoGenerator {
    SELF,
    CIRCLE
  }
}
