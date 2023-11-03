package org.stellar.reference.event.processor

import org.stellar.anchor.api.event.AnchorEvent

class NoOpEventProcessor : SepAnchorEventProcessor {
  override fun onQuoteCreated(event: AnchorEvent) {}

  override fun onTransactionCreated(event: AnchorEvent) {}

  override fun onTransactionError(event: AnchorEvent) {}

  override fun onTransactionStatusChanged(event: AnchorEvent) {}

  override fun onCustomerUpdated(event: AnchorEvent) {}
}
