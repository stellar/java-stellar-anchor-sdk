package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

@Data
public class PropertyCirclePaymentObserverConfig implements CirclePaymentObserverConfig {
  private boolean enabled = false;
  private String circleUrl;
  private String secretKey;
  private String stellarNetwork;
  private String horizonUrl;
}
