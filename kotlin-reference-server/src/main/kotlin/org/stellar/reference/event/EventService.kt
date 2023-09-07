package org.stellar.reference.event

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.stellar.reference.data.SendEventRequest
import org.stellar.reference.data.SendEventRequestPayload
import org.stellar.reference.log

class EventService {
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val receivedEvents: MutableList<SendEventRequestPayload> = mutableListOf()

  fun processEvent(receivedEvent: SendEventRequest) {
    val instant = Instant.parse(receivedEvent.timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

    log.trace("Received event created on ${dateTime.format(formatter)}")
    receivedEvents.add(receivedEvent.payload)
  }

  // Get all events. This is for testing purpose.
  // If txnId is not null, the events are filtered.
  fun getEvents(txnId: String?): List<SendEventRequestPayload> {
    if (txnId != null) {
      // filter events with txnId
      return receivedEvents.filter { it.transaction.id == txnId }
    }
    // return all events
    return receivedEvents
  }

  // Get the latest event recevied. This is for testing purpose
  fun getLatestEvent(): SendEventRequestPayload? {
    return receivedEvents.lastOrNull()
  }

  // Clear all events. This is for testing purpose
  fun clearEvents() {
    log.debug("Clearing events")
    receivedEvents.clear()
  }
}
