package org.stellar.anchor.event;

import static org.stellar.anchor.util.Log.errorF;

import io.micrometer.core.instrument.Metrics;
import java.util.Map;
import org.stellar.anchor.config.event.EventConfig;
import org.stellar.anchor.event.models.AnchorEvent;

public class EventService {
  private final EventConfig eventConfig;
  private EventPublisher eventPublisher;

  private final Map<String, String> eventTypeMapping;

  public EventService(EventConfig eventConfig) {
    this.eventConfig = eventConfig;
    this.eventTypeMapping = eventConfig.getEventTypeToQueue();
  }

  public void setEventPublisher(EventPublisher eventPublisher) {
    eventPublisher.setEventService(this);
    this.eventPublisher = eventPublisher;
  }

  public void publish(AnchorEvent event) {
    if (eventConfig.isEnabled()) {
      // publish the event
      eventPublisher.publish(event);
      // update metrics
      Metrics.counter(
              "event.published", "class", event.getClass().getSimpleName(), "type", event.getType())
          .increment();
    }
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
