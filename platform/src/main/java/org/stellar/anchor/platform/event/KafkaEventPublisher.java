package org.stellar.anchor.platform.event;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

import java.util.Properties;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.platform.config.KafkaConfig;
import org.stellar.anchor.util.Log;

/** To avoid jmx conflict, this class uses the singleton design pattern. */
@NoArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
  private static KafkaEventPublisher kafkaEventPublisher;
  Producer<String, AnchorEvent> producer;

  private KafkaEventPublisher(KafkaConfig kafkaConfig) {
    Log.debugF("kafkaConfig: {}", kafkaConfig);
    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
    props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(CLIENT_ID_CONFIG, kafkaConfig.getClientId());
    props.put(RETRIES_CONFIG, kafkaConfig.getRetries());
    props.put(LINGER_MS_CONFIG, kafkaConfig.getLingerMs());
    props.put(BATCH_SIZE_CONFIG, kafkaConfig.getBatchSize());
    // reconnect back-off is 1 second
    props.put(RECONNECT_BACKOFF_MS_CONFIG, "1000");
    // maximum reconnect back-off is 10 seconds
    props.put(RECONNECT_BACKOFF_MAX_MS_CONFIG, "10000");

    createPublisher(props);
  }

  public static KafkaEventPublisher getInstance(KafkaConfig kafkaConfig) {
    if (kafkaEventPublisher == null) {
      kafkaEventPublisher = new KafkaEventPublisher(kafkaConfig);
    }

    return kafkaEventPublisher;
  }

  protected void createPublisher(Properties props) {
    this.producer = new KafkaProducer<>(props);
  }

  @Override
  public void publish(String queue, AnchorEvent event) {
    try {
      ProducerRecord<String, AnchorEvent> record = new ProducerRecord<>(queue, event);
      record.headers().add(new RecordHeader("type", event.getType().type.getBytes()));
      // If the queue is offline, throw an exception
      try {
        producer.send(record).get();
      } catch (Exception ex) {
        throw new EventPublishException("Failed to publish event to Kafka.", ex);
      }
    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }
}
