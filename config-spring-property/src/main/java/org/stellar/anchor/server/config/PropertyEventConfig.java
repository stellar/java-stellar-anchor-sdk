package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.EventConfig;

import java.util.Map;

@Data
public class PropertyEventConfig implements EventConfig {
  private String kafkaBootstrapServer;
  private Map<String, String> queues;
  private boolean useSingleQueue;
  private boolean enabled;
}
