package org.stellar.anchor.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.*;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.KafkaEventService;
import org.stellar.anchor.event.NoopEventService;
import org.stellar.anchor.event.SqsEventService;

@Configuration
public class EventsBeans {
  @Bean
  public EventPublishService eventService(
      EventConfig eventConfig, PublisherConfig publisherConfig) {
    if (!eventConfig.isEnabled()) {
      return new NoopEventService();
    }
    // TODO handle when event publishing is disabled
    switch (eventConfig.getPublisherType()) {
      case "kafka":
        return new KafkaEventService(publisherConfig);
      case "sqs":
        return new SqsEventService(publisherConfig);
      default:
        throw new RuntimeException(
            String.format("Invalid event publisher: %s", eventConfig.getPublisherType()));
    }
  }
}
