package org.stellar.anchor.reference.event.processor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.util.Log;

/** This class is responsible for routing events to the correct SEP implementation. */
@Component
@AllArgsConstructor
public class AnchorEventProcessor {
  private final Sep6EventProcessor sep6EventProcessor;
  private final Sep24EventProcessor sep24EventProcessor;
  private final NoopEventProcessor noopEventProcessor;

  /**
   * Handles an event by routing it to the correct SEP implementation.
   *
   * @param event The event to handle
   */
  public void handleEvent(AnchorEvent event) {
    SepAnchorEventProcessor processor = getProcessor(event);
  }

  private SepAnchorEventProcessor getProcessor(AnchorEvent anchorEvent) {
    if (anchorEvent.getSep().equals("6")) {
      return sep6EventProcessor;
    } else if (anchorEvent.getSep().equals("24")) {
      return sep24EventProcessor;
    } else {
      Log.warn("Invalid SEP: " + anchorEvent.getSep());
      return noopEventProcessor;
    }
  }
}
