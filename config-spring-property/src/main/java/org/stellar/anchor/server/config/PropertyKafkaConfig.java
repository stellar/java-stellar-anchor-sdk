package org.stellar.anchor.server.config;

import java.util.Map;
import lombok.Data;
import org.stellar.anchor.config.KafkaConfig;

@Data
public class PropertyKafkaConfig implements KafkaConfig {
  private String kafkaBootstrapServer;
  private Boolean useSingleQueue;
  private Map<String, String> eventTypeToQueue;

  @Override
  public boolean isUseSingleQueue() {
    return useSingleQueue;
  }
}
