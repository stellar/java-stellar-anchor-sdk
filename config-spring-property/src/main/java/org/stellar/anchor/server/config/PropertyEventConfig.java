package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.EventConfig;

import java.util.Map;

@Data
public class PropertyEventConfig implements EventConfig {
  private String kafkaBootstrapServer = "localhost:29092";
  private Map<String, String> eventTypeToQueue = Map.of();
  private boolean useSingleQueue = false;
  private boolean enabled = false;
}
