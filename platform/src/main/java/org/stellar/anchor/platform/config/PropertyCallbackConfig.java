package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.CallbackConfig;

@Data
public class PropertyCallbackConfig implements CallbackConfig {
  String endpoint;
}
