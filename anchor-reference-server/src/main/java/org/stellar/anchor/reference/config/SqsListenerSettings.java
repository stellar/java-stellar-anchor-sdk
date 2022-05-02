package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sqs.listener")
public class SqsListenerSettings {
  String region;
  Boolean useSingleQueue;
  Queues eventTypeToQueue;
  String accessKey;
  String secretKey;

  @Data
  public static class Queues {
    String all;
    String quoteCreated;
    String transactionCreated;
    String transactionStatusChanged;
    String transactionError;
  }

  public Boolean isUseSingleQueue() {
    return useSingleQueue;
  }
}
