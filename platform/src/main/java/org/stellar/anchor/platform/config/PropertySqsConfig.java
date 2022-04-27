package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.SqsConfig;

import java.util.Map;

@Data
public class PropertySqsConfig implements SqsConfig {
  private String region;
  private Boolean useSingleQueue;
  private Map<String, String> eventTypeToQueue;
  private String accessKey;
  private String secretKey;

  @Override
  public Boolean isUseSingleQueue() {
    return useSingleQueue;
  }
}
