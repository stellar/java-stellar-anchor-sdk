package org.stellar.anchor.platform.config;

import lombok.Data;

@Data
public class PaymentObserverConfig {
  boolean enabled;
  PaymentObserverType type;
  StellarPaymentObserverConfig stellar;

  public enum PaymentObserverType {
    STELLAR
  }

  @Data
  public static class StellarPaymentObserverConfig {
    int silenceCheckInterval;
    int silenceTimeout;
    int silenceTimeoutRetries;
    int initialStreamBackoffTime;
    int maxStreamBackoffTime;
    int initialEventBackoffTime;
    int maxEventBackoffTime;
  }
}
