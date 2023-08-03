package org.stellar.anchor.reference.event;

import org.stellar.anchor.api.event.AnchorEvent;

public class NoopEventProcessor implements IAnchorEventProcessor {
  @Override
  public void onQuoteCreatedEvent(AnchorEvent event) {}

  @Override
  public void onTransactionCreated(AnchorEvent event) {}

  @Override
  public void onTransactionError(AnchorEvent event) {}

  @Override
  public void onTransactionStatusChanged(AnchorEvent event) {}

  @Override
  public void onKycUpdatedEvent(AnchorEvent event) {}
}
