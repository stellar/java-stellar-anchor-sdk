package org.stellar.anchor.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;

public interface EventService {
  // To be deprecated
  void publish(AnchorEvent event) throws EventPublishException;
  //
  //  Session createSession();
  //
  //  interface Session {
  //    /**
  //     * Publishes an event to the event queue. The queue will be determined by the implementation
  // of the Session.
  //     *
  //     * @param event the event to publish
  //     */
  //    void publish(AnchorEvent event);
  //
  //    /**
  //     * Reads events from the event queue.
  //     * @return
  //     */
  //    ReadResponse read();
  //    void ack(ReadResponse readResponse);
  //  }
  //
  //  interface ReadResponse {
  //    List<AnchorEvent> getEvents();
  //  }
}
