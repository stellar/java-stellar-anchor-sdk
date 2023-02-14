package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.listener")
public class KafkaListenerSettings {
  Boolean enabled = true;
  String bootStrapServer;
  Boolean useSingleQueue;
  Boolean useIAM = false;
  Queues eventTypeToQueue;

  @Data
  public static class Queues {
    String all;
    String quoteCreated;
    String transactionCreated;
    String transactionStatusChanged;
    String transactionError;
  }

  public boolean isUseIAM() {
    return useIAM;
  }
}
