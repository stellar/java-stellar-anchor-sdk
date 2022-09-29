package org.stellar.anchor.config;

import org.springframework.validation.Errors;

public interface PaymentObserverConfig {
  boolean isEnabled();

  String getTrackedWallet();

  String getCircleUrl();
  String getApiKey();
}
