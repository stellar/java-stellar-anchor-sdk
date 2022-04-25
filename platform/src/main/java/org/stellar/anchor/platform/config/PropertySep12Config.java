package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.Sep12Config;

@Data
public class PropertySep12Config implements Sep12Config {
  Boolean enabled;
  String customerIntegrationEndPoint;
}
