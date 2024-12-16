package org.stellar.reference.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.stellar.reference.data.SendEventRequest
import org.stellar.reference.event.processor.AnchorEventProcessor
import org.stellar.reference.log

@OptIn(ExperimentalCoroutinesApi::class)
class EventConsumer(
  private val channel: Channel<SendEventRequest>,
  private val processor: AnchorEventProcessor,
) {
  private var stopped = false

  suspend fun start(): EventConsumer {
    while (!stopped) {
      while (!channel.isEmpty) {
        val event = channel.receive()
        log.info { "Processing event ${event.id} of type ${event.type}" }
        processor.handleEvent(event)
      }
    }
    return this
  }

  fun stop() {
    stopped = true
  }
}
