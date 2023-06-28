package org.stellar.anchor.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;

public interface EventService {
  void publish(AnchorEvent event) throws EventPublishException;
}
