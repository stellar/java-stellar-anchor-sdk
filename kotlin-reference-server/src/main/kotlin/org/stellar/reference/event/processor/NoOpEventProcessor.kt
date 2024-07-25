package org.stellar.reference.event.processor

import org.stellar.reference.data.SendEventRequest

class NoOpEventProcessor : SepAnchorEventProcessor {
  override suspend fun onQuoteCreated(event: SendEventRequest) {}

  override suspend fun onTransactionCreated(event: SendEventRequest) {}

  override suspend fun onTransactionStatusChanged(event: SendEventRequest) {}

  override suspend fun onCustomerUpdated(event: SendEventRequest) {}
}
