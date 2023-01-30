package org.stellar.anchor.platform.event;

import static org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_CREATED;
import static org.stellar.anchor.util.Log.errorF;

import io.micrometer.core.instrument.Metrics;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.config.event.EventConfig;
import org.stellar.anchor.event.EventPublisher;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.TransactionHelper;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public class DefaultEventService implements EventService {
  private final EventConfig eventConfig;
  private EventPublisher eventPublisher;

  private final Map<String, String> eventTypeMapping;

  public DefaultEventService(EventConfig eventConfig) {
    this.eventConfig = eventConfig;
    this.eventTypeMapping = eventConfig.getEventTypeToQueue();
  }

  @Override
  @SneakyThrows
  public void publish(Sep24Transaction txn, AnchorEvent.Type type) {
    JdbcSepTransaction jdbcTxn = (JdbcSepTransaction) txn;
    AnchorEvent event =
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("24")
            .type(TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(jdbcTxn))
            .build();
    publish(event);
  }

  @Override
  @SneakyThrows
  public void publish(Sep31Transaction txn, AnchorEvent.Type type) {
    JdbcSepTransaction jdbcTxn = (JdbcSepTransaction) txn;
    AnchorEvent event =
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("31")
            .type(type)
            .transaction(TransactionHelper.toGetTransactionResponse(jdbcTxn))
            .build();
    publish(event);
  }

  @Override
  public void publish(AnchorEvent event) throws EventPublishException {
    if (eventConfig.isEnabled()) {
      // publish the event
      eventPublisher.publish(getQueue(event.getType().type), event);
      // update metrics
      Metrics.counter(
              "event.published",
              "class",
              event.getClass().getSimpleName(),
              "type",
              event.getType().type)
          .increment();
    }
  }

  public void setEventPublisher(EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  String getQueue(String eventType) {
    String queue = eventTypeMapping.get(eventType);
    if (queue == null) {
      errorF("There is no queue defined for event type:{}", eventType);
      throw new RuntimeException(
          String.format("There is no queue defined for event type:%s", eventType));
    }
    return queue;
  }
}
