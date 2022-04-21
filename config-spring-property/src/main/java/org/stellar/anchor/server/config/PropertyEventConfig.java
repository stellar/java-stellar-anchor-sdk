package org.stellar.anchor.server.config;


import lombok.Data;
import org.stellar.anchor.config.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig {
  private boolean useSingleQueue = false;
  private boolean enabled = false;
  private String queueType;
}
