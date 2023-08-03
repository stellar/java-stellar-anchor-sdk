package org.stellar.reference.wallet.callback

import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.reference.wallet.log

class CallbackEventService {
  private val receivedCallbacks: MutableList<Sep24GetTransactionResponse> = mutableListOf()

  fun processCallback(receivedCallback: Sep24GetTransactionResponse) {
    receivedCallbacks.add(receivedCallback)
  }

  // Get all events. This is for testing purpose.
  // If txnId is not null, the events are filtered.
  fun getCallbacks(txnId: String?): List<Sep24GetTransactionResponse> {
    if (txnId != null) {
      // filter events with txnId
      return receivedCallbacks.filter { it.transaction.id == txnId }
    }
    // return all events
    return receivedCallbacks
  }

  // Get the latest event recevied. This is for testing purpose
  fun getLatestEvent(): Sep24GetTransactionResponse? {
    return receivedCallbacks.lastOrNull()
  }

  // Clear all events. This is for testing purpose
  fun clearEvents() {
    log.debug("Clearing events")
    receivedCallbacks.clear()
  }
}
