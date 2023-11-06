package org.stellar.anchor.platform.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;

public interface EventPublisher {
  void publish(String queue, AnchorEvent event) throws EventPublishException;
}
