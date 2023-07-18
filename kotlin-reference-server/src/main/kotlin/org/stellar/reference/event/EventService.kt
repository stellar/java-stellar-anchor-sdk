package org.stellar.reference.event

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.reference.data.SendEventRequest

class EventService {
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val receivedEvents: MutableList<AnchorEvent> = mutableListOf()
  fun processEvent(receivedEvent: SendEventRequest) {
    val instant = Instant.ofEpochSecond(receivedEvent.timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

    println("Received event created on ${dateTime.format(formatter)}")
    receivedEvents.add(receivedEvent.payload)
  }

  // Get all events. This is for testing purpose.
  fun getEvents(): List<AnchorEvent> {
    return receivedEvents
  }

  // Get the latest event recevied. This is for testing purpose
  fun getLatestEvent(): AnchorEvent? {
    return receivedEvents.lastOrNull()
  }

  // Clear all events. This is for testing purpose
  fun clearEvents() {
    receivedEvents.clear()
  }
}
