package org.stellar.anchor.platform.component.share;

import static org.stellar.anchor.util.Log.errorF;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.PropertyEventConfig;
import org.stellar.anchor.platform.event.KafkaEventPublisher;
import org.stellar.anchor.platform.event.MskEventPublisher;
import org.stellar.anchor.platform.event.NoopEventPublisher;
import org.stellar.anchor.platform.event.SqsEventPublisher;
import org.stellar.anchor.platform.service.PaymentOperationToEventListener;
import org.stellar.anchor.sep31.Sep31TransactionStore;

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

  @Bean
  public PaymentOperationToEventListener paymentOperationToEventListener(
      Sep31TransactionStore transactionStore, EventService eventService) {
    // TODO: Remove this bean
    return new PaymentOperationToEventListener(transactionStore, eventService);
  }
}
