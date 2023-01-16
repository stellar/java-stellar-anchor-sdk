package org.stellar.anchor.platform.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.event.EventPublisher;

public class NoopEventPublisher implements EventPublisher {
  @Override
  public void publish(String queue, AnchorEvent event) {
    debugF("Event ID={} is published to NOOP class.", event.getEventId());
  }
}
