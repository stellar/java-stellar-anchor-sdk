package org.stellar.anchor.reference.event;

import org.stellar.anchor.api.event.AnchorEvent;

public interface IAnchorEventProcessor {
  void onQuoteCreatedEvent(AnchorEvent event);

  void onTransactionCreated(AnchorEvent event);

  void onTransactionError(AnchorEvent event);

  void onTransactionStatusChanged(AnchorEvent event);

  void onKycUpdatedEvent(AnchorEvent event);
}
