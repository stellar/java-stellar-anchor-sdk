package org.stellar.anchor.paymentservice.circle.config;

public interface CirclePaymentConfig {
  String getName();

  boolean isEnabled();

  /**
   * @return the Circle API base URL. It's usually `https://api-sandbox.circle.com/` for the sandbox.
   */
  String getCircleUrl();

  /**
   * @return the API key of the Circle account.
   */
  String getSecretKey();

  /**
   * @return The Stellar horizon URL. The default testnet url is `https://horizon-testnet.stellar.org` and the default
   * pubnet one is `https://horizon.stellar.org`.
   */
  String getHorizonUrl();

  /**
   * @return "TESTNET" or "PUBLIC".
   */
  String getStellarNetwork();
}
