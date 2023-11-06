package org.stellar.anchor.client.config;

import lombok.Data;
import org.stellar.anchor.config.Sep12Config;

@Data
public class PropertySep12Config implements Sep12Config {
  Boolean enabled;
  String customerIntegrationEndPoint;

  public PropertySep12Config(CallbackApiConfig callbackApiConfig) {
    this.customerIntegrationEndPoint = callbackApiConfig.getBaseUrl();
  }
}
