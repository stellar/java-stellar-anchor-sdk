package org.stellar.anchor.platform.config;

import lombok.Data;

@Data
public class PlatformServerConfig {
  String contextPath;
  PropertySecretConfig secretConfig;

  public PlatformServerConfig(PropertySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }
}
