package org.stellar.reference.wallet.data

import org.stellar.anchor.api.event.AnchorEvent

data class SendEventRequest(val timestamp: Long, val payload: AnchorEvent)

data class JwtToken(
  val transactionId: String,
  var expiration: Long, // Expiration Time
  var data: Map<String, String>,
)
