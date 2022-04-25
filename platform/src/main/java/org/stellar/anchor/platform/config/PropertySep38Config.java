package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.Sep38Config;

@Data
public class PropertySep38Config implements Sep38Config {
  boolean enabled = false;
  String quoteIntegrationEndPoint;
}
