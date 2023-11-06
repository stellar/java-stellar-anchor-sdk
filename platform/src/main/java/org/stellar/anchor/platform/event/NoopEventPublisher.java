package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debugF;

import org.stellar.anchor.api.event.AnchorEvent;

public class NoopEventPublisher implements EventPublisher {
  @Override
  public void publish(String queue, AnchorEvent event) {
    debugF("Event ID={} is published to NOOP class.", event.getId());
  }
}
