package org.stellar.anchor.platform.payment.config;

import lombok.Data;

@Data
public class CirclePaymentConfig {
  private String name = "";
  private boolean enabled = false;
}
