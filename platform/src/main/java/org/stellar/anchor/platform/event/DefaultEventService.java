package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.errorF;

import io.micrometer.core.instrument.Metrics;
import java.util.Map;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.event.EventConfig;
import org.stellar.anchor.event.EventService;

public class DefaultEventService implements EventService {
  private final EventConfig eventConfig;
  private EventPublisher eventPublisher;
  private AssetService assetService;

  private final Map<String, String> eventTypeMapping;

  public DefaultEventService(
      EventConfig eventConfig, AssetService assetService, EventPublisher eventPublisher) {
    this.eventConfig = eventConfig;
    this.eventTypeMapping = eventConfig.getEventTypeToQueue();
    this.assetService = assetService;
    this.eventPublisher = eventPublisher;
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
