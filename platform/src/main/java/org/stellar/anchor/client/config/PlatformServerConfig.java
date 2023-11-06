package org.stellar.anchor.client.config;

import lombok.Data;
import org.stellar.anchor.auth.AuthConfig;

@Data
public class PlatformServerConfig {
  String contextPath;
  AuthConfig auth;
  PropertySecretConfig secretConfig;

  public PlatformServerConfig(PropertySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }
}
