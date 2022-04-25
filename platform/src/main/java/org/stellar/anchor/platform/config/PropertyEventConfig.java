package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
import org.stellar.anchor.config.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig {
  private String kafkaBootstrapServer = "localhost:29092";
  private Map<String, String> eventTypeToQueue = Map.of();
  private boolean useSingleQueue = false;
  private boolean enabled = false;
}
