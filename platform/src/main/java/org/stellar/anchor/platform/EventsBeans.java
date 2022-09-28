package org.stellar.anchor.platform;

import static org.stellar.anchor.util.Log.errorF;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.event.*;
import org.stellar.anchor.platform.config.PropertyEventConfig;

@Configuration
public class EventsBeans {
  @Bean
  public EventService eventService(PropertyEventConfig eventConfig) throws InvalidConfigException {
    EventService eventService = new EventService(eventConfig);
    if (!eventConfig.isEnabled()) {
      eventService.setEventPublisher(new NoopEventPublisher());
    }
    String publisherType = eventConfig.getPublisher().getType();
    switch (publisherType) {
      case "kafka":
        eventService.setEventPublisher(
            new KafkaEventPublisher(eventConfig.getPublisher().getKafka()));
        break;
      case "sqs":
        eventService.setEventPublisher(new SqsEventPublisher(eventConfig.getPublisher().getSqs()));
        break;
      case "msk":
        eventService.setEventPublisher(new MskEventPublisher(eventConfig.getPublisher().getMsk()));
        break;
      default:
        errorF("Invalid event publisher: {}", publisherType);
        throw new InvalidConfigException(
            String.format("Invalid event publisher: %s", publisherType));
    }

    return eventService;
  }
}
