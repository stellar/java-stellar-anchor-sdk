package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.Sep31Config;

@Data
public class PropertySep31Config implements Sep31Config {
  boolean enabled = false;
  String feeIntegrationEndPoint = "http://localhost:8081";
}
