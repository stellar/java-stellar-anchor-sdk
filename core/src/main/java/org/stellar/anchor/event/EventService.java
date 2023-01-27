package org.stellar.anchor.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public interface EventService {
  void publish(Sep24Transaction txn, AnchorEvent.Type type);

  void publish(Sep31Transaction txn, AnchorEvent.Type type);

  void publish(AnchorEvent event) throws EventPublishException;
}
