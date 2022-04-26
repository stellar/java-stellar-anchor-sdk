package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig {
  private boolean enabled = false;
  private String publisherType;
}
