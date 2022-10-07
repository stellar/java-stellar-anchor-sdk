package org.stellar.anchor.event;

import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.event.models.AnchorEvent;

public interface EventPublisher {
  void publish(String queue, AnchorEvent event) throws EventPublishException;
}
