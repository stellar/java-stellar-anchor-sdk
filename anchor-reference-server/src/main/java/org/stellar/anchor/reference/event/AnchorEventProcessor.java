package org.stellar.anchor.reference.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.util.Log;

@Component
@AllArgsConstructor
public class AnchorEventProcessor {
  private final Sep6EventProcessor sep6EventProcessor;
  private final Sep24EventProcessor sep24EventProcessor;
  private final NoopEventProcessor noopEventProcessor;

  public void handleEvent(AnchorEvent event) {
    IAnchorEventProcessor processor = getProcessor(event);

    switch (event.getType()) {
      case TRANSACTION_CREATED:
        processor.onTransactionCreated(event);
        break;
      case TRANSACTION_STATUS_CHANGED:
        processor.onTransactionStatusChanged(event);
        break;
      case TRANSACTION_ERROR:
        processor.onTransactionError(event);
        break;
      case QUOTE_CREATED:
        processor.onQuoteCreatedEvent(event);
        break;
      case KYC_UPDATED:
        // Assume all KYC update events are for SEP-6
        sep6EventProcessor.onKycUpdatedEvent(event);
        break;
      default:
        Log.warn("Invalid event type: " + event.getType());
    }
  }

  private IAnchorEventProcessor getProcessor(AnchorEvent anchorEvent) {
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
