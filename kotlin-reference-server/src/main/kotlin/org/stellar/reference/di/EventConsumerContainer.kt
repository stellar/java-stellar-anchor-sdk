package org.stellar.reference.di

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.stellar.reference.event.EventConsumer
import org.stellar.reference.event.processor.AnchorEventProcessor
import org.stellar.reference.event.processor.NoOpEventProcessor
import org.stellar.reference.event.processor.Sep6EventProcessor

object EventConsumerContainer {
  val config = ConfigContainer.getInstance().config
  private val consumerConfig =
    mapOf(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to config.eventSettings.bootstrapServer,
      ConsumerConfig.GROUP_ID_CONFIG to "anchor-event-consumer",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    )
  private val kafkaConsumer =
    KafkaConsumer<String, String>(consumerConfig).also {
      it.subscribe(listOf(config.eventSettings.topic))
    }
  private val sep6EventProcessor =
    Sep6EventProcessor(config, ServiceContainer.horizon, ServiceContainer.platform)
  private val noOpEventProcessor = NoOpEventProcessor()
  private val processor = AnchorEventProcessor(sep6EventProcessor, noOpEventProcessor)
  val eventConsumer = EventConsumer(kafkaConsumer, processor)
}
