package org.stellar.anchor.server.config;

import java.util.Map;
import lombok.Data;
import org.stellar.anchor.config.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig {
  private String kafkaBootstrapServer;
  private Map<String, String> queues;
  private boolean useSingleQueue;
  private boolean enabled;
}
