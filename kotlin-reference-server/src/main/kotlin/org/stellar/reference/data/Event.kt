package org.stellar.reference.data

import org.stellar.anchor.api.event.AnchorEvent

data class SendEventRequest(val timestamp: Long, val payload: AnchorEvent)
