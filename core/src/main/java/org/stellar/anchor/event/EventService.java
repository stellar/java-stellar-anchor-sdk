package org.stellar.anchor.event;

import org.stellar.anchor.event.models.AnchorEvent;

public interface EventService {
  void publish(AnchorEvent event);
}
