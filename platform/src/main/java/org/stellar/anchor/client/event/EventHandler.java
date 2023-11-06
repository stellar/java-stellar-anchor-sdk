package org.stellar.anchor.client.event;

import java.io.IOException;
import org.stellar.anchor.api.event.AnchorEvent;

public abstract class EventHandler {

  /**
   * Handle the event. Return true if the event was delivered and the downstream server responded
   * HTTP status 200, otherwise return false.
   *
   * @param event the event to handle
   * @return true if the event was delivered and the downstream server responded HTTP status 200,
   * @throws IOException if there was a network error sending the event.
   */
  abstract boolean handleEvent(AnchorEvent event) throws IOException;
}
