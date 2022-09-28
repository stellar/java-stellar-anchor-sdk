package org.stellar.anchor.event;

import static org.stellar.anchor.util.Log.debugF;

import org.stellar.anchor.event.models.AnchorEvent;

public class NoopEventPublisher extends EventPublisher {
  @Override
  public void publish(AnchorEvent event) {
    debugF("Event ID={} is published to NOOP class.", event.getEventId());
  }
}
