package org.stellar.reference.event.processor

import org.stellar.anchor.api.event.AnchorEvent

interface SepAnchorEventProcessor {
  fun onQuoteCreated(event: AnchorEvent)
  fun onTransactionCreated(event: AnchorEvent)
  fun onTransactionError(event: AnchorEvent)
  fun onTransactionStatusChanged(event: AnchorEvent)
  fun onCustomerUpdated(event: AnchorEvent)
}
