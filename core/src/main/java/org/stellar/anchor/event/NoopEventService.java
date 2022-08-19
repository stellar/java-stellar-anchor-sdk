package org.stellar.anchor.event;

import org.stellar.anchor.event.models.AnchorEvent;

public class NoopEventService implements EventPublishService {
  @Override
  public void publish(AnchorEvent event) {
    // noop
  }
}
