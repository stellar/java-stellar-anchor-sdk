package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig {
  private boolean enabled = false;
  private String publisherType;
}
