package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debugF;

import org.stellar.anchor.event.EventPublisher;
import org.stellar.anchor.event.models.AnchorEvent;

public class NoopEventPublisher implements EventPublisher {
  @Override
  public void publish(String queue, AnchorEvent event) {
    debugF("Event ID={} is published to NOOP class.", event.getEventId());
  }
}
