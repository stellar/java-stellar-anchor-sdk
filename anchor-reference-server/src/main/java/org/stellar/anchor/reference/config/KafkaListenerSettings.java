package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.listener")
public class KafkaListenerSettings {
  String bootStrapServer;
  Boolean useSingleQueue;
  Queues eventTypeToQueue;

  @Data
  public static class Queues {
    String all;
    String quoteCreated;
    String transactionCreated;
    String transactionPaymentReceived;
    String transactionError;
  }
}
