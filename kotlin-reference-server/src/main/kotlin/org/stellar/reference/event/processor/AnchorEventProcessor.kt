package org.stellar.reference.event.processor

import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.reference.log

class AnchorEventProcessor(
  private val sep6EventProcessor: Sep6EventProcessor,
  private val noOpEventProcessor: NoOpEventProcessor
) {
  fun handleEvent(event: AnchorEvent) {
    val processor = getProcessor(event)
    try {
      when (event.type) {
        AnchorEvent.Type.TRANSACTION_CREATED -> {
          log.info("Received transaction created event")
          processor.onTransactionCreated(event)
        }
        AnchorEvent.Type.TRANSACTION_STATUS_CHANGED -> {
          log.info("Received transaction status changed event")
          processor.onTransactionStatusChanged(event)
        }
        AnchorEvent.Type.TRANSACTION_ERROR -> {
          log.info("Received transaction error event")
          processor.onTransactionError(event)
        }
        AnchorEvent.Type.CUSTOMER_UPDATED -> {
          log.info("Received customer updated event")
          // Only SEP-6 listens to this event
          sep6EventProcessor.onCustomerUpdated(event)
        }
        AnchorEvent.Type.QUOTE_CREATED -> {
          log.info("Received quote created event")
          processor.onQuoteCreated(event)
        }
        else -> {
          log.warn(
            "Received event of type ${event.type} which is not supported by the reference server"
          )
        }
      }
    } catch (e: Exception) {
      log.error("Error processing event: $event", e)
    }
  }

  private fun getProcessor(event: AnchorEvent): SepAnchorEventProcessor =
    when (event.sep) {
      "6" -> sep6EventProcessor
      else -> noOpEventProcessor
    }
}
