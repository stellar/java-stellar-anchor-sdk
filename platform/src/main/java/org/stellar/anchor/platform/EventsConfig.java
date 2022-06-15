package org.stellar.anchor.platform;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.*;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.KafkaEventService;
import org.stellar.anchor.event.SqsEventService;

@Configuration
@AutoConfigureOrder(2)
public class EventsConfig {
  @Bean
  @ConditionalOnProperty(value = "app-config.event.enabled", havingValue = "true")
  public EventPublishService eventService(
      EventConfig eventConfig, KafkaConfig kafkaConfig, SqsConfig sqsConfig) {
    // TODO handle when event publishing is disabled
    switch (eventConfig.getPublisherType()) {
      case "kafka":
        return new KafkaEventService(kafkaConfig);
      case "sqs":
        return new SqsEventService(sqsConfig);
      default:
        throw new RuntimeException(
            String.format("Invalid event publisher: %s", eventConfig.getPublisherType()));
    }
  }
}
