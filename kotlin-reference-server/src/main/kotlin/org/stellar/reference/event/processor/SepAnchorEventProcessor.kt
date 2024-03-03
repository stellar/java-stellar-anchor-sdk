package org.stellar.reference.event.processor

import org.stellar.reference.data.SendEventRequest

interface SepAnchorEventProcessor {
  suspend fun onQuoteCreated(event: SendEventRequest)

  suspend fun onTransactionCreated(event: SendEventRequest)

  suspend fun onTransactionError(event: SendEventRequest)

  suspend fun onTransactionStatusChanged(event: SendEventRequest)

  suspend fun onCustomerUpdated(event: SendEventRequest)
}
