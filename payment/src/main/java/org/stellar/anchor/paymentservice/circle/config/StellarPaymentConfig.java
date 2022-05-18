package org.stellar.anchor.paymentservice.circle.config;

/**
 * This is a placeholder class. TODO: move to payment-stellar subproject under
 * org.stellar.anchor.paymentservice.circle.config package.
 */
public interface StellarPaymentConfig {
  String getName();

  boolean isEnabled();

  String getHorizonUrl();

  String getSecretKey();
}
