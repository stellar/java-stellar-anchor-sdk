package org.stellar.reference.di

import org.stellar.reference.event.EventConsumer
import org.stellar.reference.event.processor.AnchorEventProcessor
import org.stellar.reference.event.processor.NoOpEventProcessor
import org.stellar.reference.event.processor.Sep6EventProcessor

object EventConsumerContainer {
  val config = ConfigContainer.getInstance().config
  private val sep6EventProcessor =
    Sep6EventProcessor(
      config,
      ServiceContainer.horizon,
      ServiceContainer.platform,
      ServiceContainer.customerService,
      ServiceContainer.sepHelper,
    )
  private val noOpEventProcessor = NoOpEventProcessor()
  private val processor = AnchorEventProcessor(sep6EventProcessor, noOpEventProcessor)
  val eventConsumer = EventConsumer(ServiceContainer.eventService.channel, processor)
}
