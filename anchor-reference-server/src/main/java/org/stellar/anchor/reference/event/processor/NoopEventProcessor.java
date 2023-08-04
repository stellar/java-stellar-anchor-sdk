package org.stellar.anchor.reference.event.processor;

import org.stellar.anchor.api.event.AnchorEvent;

public class NoopEventProcessor implements SepAnchorEventProcessor {
  @Override
  public void onQuoteCreatedEvent(AnchorEvent event) {}

  @Override
  public void onTransactionCreated(AnchorEvent event) {}

  @Override
  public void onTransactionError(AnchorEvent event) {}

  @Override
  public void onTransactionStatusChanged(AnchorEvent event) {}

  @Override
  public void onCustomerUpdated(AnchorEvent event) {}
}
