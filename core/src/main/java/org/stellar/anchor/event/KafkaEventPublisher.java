package org.stellar.anchor.event;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.config.event.KafkaConfig;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.util.Log;

public class KafkaEventPublisher extends EventPublisher {
  Producer<String, AnchorEvent> producer;

  public KafkaEventPublisher(KafkaConfig kafkaConfig) {
    Log.debugF("kafkaConfig: {}", kafkaConfig);
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    createPublisher(props);
  }

  protected void createPublisher(Properties props) {
    this.producer = new KafkaProducer<>(props);
  }

  @Override
  public void publish(AnchorEvent event) {
    try {
      String topic = eventService.getQueue(event.getType());
      ProducerRecord<String, AnchorEvent> record = new ProducerRecord<>(topic, event);
      record.headers().add(new RecordHeader("type", event.getType().getBytes()));
      producer.send(record);
    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }
}
