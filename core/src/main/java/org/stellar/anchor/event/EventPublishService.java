package org.stellar.anchor.event;

import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.event.models.AnchorEvent;

public interface EventPublishService {
  void publish(AnchorEvent event) throws EventPublishException;
}
