package org.stellar.anchor.server.config.payment;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.config.StellarxPaymentConfig;

@Data
public class PropertyStellarxPaymentConfig implements StellarxPaymentConfig {
  private String name = "";
  private boolean enabled = false;
  private String horizonUrl;
  private String secretKey;
}
