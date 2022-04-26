package org.stellar.anchor.payment.config;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.config.CirclePaymentConfig;

@Data
public class PropertyCirclePaymentConfig implements CirclePaymentConfig {
  private String name = "";
  private boolean enabled = false;
  private String circleUrl;
  private String secretKey;
  private String horizonUrl;
  private String stellarNetwork = "TESTNET";
}
