package org.stellar.anchor.server.config.payment;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.config.StellarPaymentConfig;

@Data
public class PropertyStellarPaymentConfig implements StellarPaymentConfig {
  private String name = "";
  private boolean enabled = false;
  private String horizonUrl;
  private String secretKey;
}
