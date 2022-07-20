package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
import org.stellar.anchor.config.KafkaConfig;

@Data
public class PropertyKafkaConfig implements KafkaConfig {
  private String bootstrapServer;
  private Boolean useSingleQueue;
  private Boolean useIAM = false;
  private Map<String, String> eventTypeToQueue;

  @Override
  public boolean isUseSingleQueue() {
    return useSingleQueue;
  }

  @Override
  public boolean isUseIAM() {
    return useIAM;
  }
}
