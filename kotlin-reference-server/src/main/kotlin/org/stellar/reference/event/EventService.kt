package org.stellar.reference.event

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.channels.Channel
import org.stellar.reference.data.SendEventRequest
import org.stellar.reference.log

class EventService() {
  val channel = Channel<SendEventRequest>()
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  suspend fun processEvent(receivedEvent: SendEventRequest) {
    val instant = Instant.parse(receivedEvent.timestamp)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

    log.info(
      "Received event ${receivedEvent.id} of type ${receivedEvent.type} at ${dateTime.format(formatter)}"
    )
    channel.send(receivedEvent)
  }
}
