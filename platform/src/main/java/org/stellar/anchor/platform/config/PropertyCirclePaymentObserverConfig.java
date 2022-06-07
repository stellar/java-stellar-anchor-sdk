package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

@Data
public class PropertyCirclePaymentObserverConfig implements CirclePaymentObserverConfig {
  private boolean enabled = false;
  private String stellarNetwork = "TESTNET";
  private String horizonUrl = "https://horizon-testnet.stellar.org";
  private String trackedWallet = "all";
}
