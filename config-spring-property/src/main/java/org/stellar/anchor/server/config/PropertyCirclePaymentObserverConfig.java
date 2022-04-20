package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

@Data
public class PropertyCirclePaymentObserverConfig implements CirclePaymentObserverConfig {
  private boolean enabled = false;
  private String circleUrl;
  private String secretKey;
}
