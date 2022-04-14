package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.Sep31Config;

import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;

@Data
public class PropertySep31Config implements Sep31Config {
  boolean enabled = false;
  String feeIntegrationEndPoint = "http://localhost:8081";
  PaymentType paymentType = STRICT_SEND;
}
