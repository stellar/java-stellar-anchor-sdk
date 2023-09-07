package org.stellar.reference.wallet

import com.google.gson.JsonObject
import java.time.Duration
import java.time.Instant
import java.util.*
import org.stellar.sdk.KeyPair

class CallbackService {
  private val receivedCallbacks: MutableList<JsonObject> = mutableListOf()

  fun processCallback(receivedCallback: JsonObject) {
    receivedCallbacks.add(receivedCallback)
  }

  // Get all events. This is for testing purpose.
  // If txnId is not null, the events are filtered.
  fun getCallbacks(txnId: String?): List<JsonObject> {
    if (txnId != null) {
      // filter events with txnId
      return receivedCallbacks.filter {
        it.getAsJsonObject("transaction")?.get("id")?.asString == txnId
      }
    }
    // return all events
    return receivedCallbacks
  }

  // Get the latest event received. This is for testing purpose
  fun getLatestCallback(): JsonObject? {
    return receivedCallbacks.lastOrNull()
  }

  // Clear all events. This is for testing purpose
  fun clear() {
    log.debug("Clearing events")
    receivedCallbacks.clear()
  }

  companion object {
    fun verifySignature(
      header: String?,
      body: String?,
      domain: String?,
      signer: KeyPair?
    ): Boolean {
      if (header == null) {
        return false
      }
      val tokens = header.split(",")
      if (tokens.size != 2) {
        return false
      }
      // t=timestamp
      val timestampTokens = tokens[0].trim().split("=")
      if (timestampTokens.size != 2 || timestampTokens[0] != "t") {
        return false
      }
      val timestampLong = timestampTokens[1].trim().toLongOrNull() ?: return false
      val timestamp = Instant.ofEpochSecond(timestampLong)

      if (Duration.between(timestamp, Instant.now()).toMinutes() > 2) {
        // timestamp is older than 2 minutes
        return false
      }

      // s=signature
      val sigTokens = tokens[1].trim().split("=", limit = 2)
      if (sigTokens.size != 2 || sigTokens[0] != "s") {
        return false
      }

      val sigBase64 = sigTokens[1].trim()
      if (sigBase64.isEmpty()) {
        return false
      }

      val signature = Base64.getDecoder().decode(sigBase64)

      if (body == null) {
        return false
      }

      val payloadToVerify = "${timestampLong}.${domain}.${body}"
      if (signer == null) {
        return false
      }

      if (!signer.verify(payloadToVerify.toByteArray(), signature)) {
        return false
      }

      return true
    }
  }
}
