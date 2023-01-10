package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Data
@Configuration
@ConfigurationProperties(prefix = "queue.kafka")
public class EventProcessorConfig implements Validator {
  QueueConfig queue;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return EventProcessorConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    // TODO: Add validations after implementation
  }

  @Data
  public static class QueueConfig {
    String type;
    KafkaConfig kafka;
  }

  @Data
  public static class KafkaConfig {
    String bootStrapServer;
    KafkaTopicMapping eventTypeToTopic;
  }

  @Data
  public static class KafkaTopicMapping {
    String all;
    String quoteCreated;
    String transactionCreated;
    String transactionStatusUpdated;
    String transactionError;
  }
}
