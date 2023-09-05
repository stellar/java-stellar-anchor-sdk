package org.stellar.reference.event

import java.time.Duration
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.event.processor.AnchorEventProcessor
import org.stellar.reference.log

class EventConsumer(
  private val consumer: KafkaConsumer<String, String>,
  private val processor: AnchorEventProcessor
) {
  fun start() {
    while (true) {
      val records = consumer.poll(Duration.ofSeconds(10))
      if (!records.isEmpty) {
        log.info("Received ${records.count()} records")
        records.forEach { record ->
          processor.handleEvent(
            GsonUtils.getInstance().fromJson(record.value(), AnchorEvent::class.java)
          )
        }
      }
    }
  }
}
