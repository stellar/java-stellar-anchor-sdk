package org.stellar.anchor.platform.event;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.EventService.EventQueue;
import org.stellar.anchor.platform.config.KafkaConfig;
import org.stellar.anchor.util.Log;

public class KafkaSession implements EventService.Session {

  final KafkaConfig kafkaConfig;
  final String sessionName;
  final String topic;
  Producer<String, AnchorEvent> producer = null;
  Consumer<String, AnchorEvent> consumer = null;

  KafkaSession(KafkaConfig kafkaConfig, String sessionName, EventQueue queue) {
    this.kafkaConfig = kafkaConfig;
    this.sessionName = sessionName;
    this.topic = queue.name();
  }

  @Override
  public void publish(AnchorEvent event) throws AnchorException {
    try {
      if (producer == null) {
        producer = createProducer();
      }
      ProducerRecord<String, AnchorEvent> record = new ProducerRecord<>(topic, event);
      record.headers().add(new RecordHeader("type", event.getType().type.getBytes()));
      // If the queue is offline, throw an exception
      try {
        producer.send(record).get();
      } catch (Exception ex) {
        throw new EventPublishException("Failed to publish event to Kafka.", ex);
      }

      // publish the event to the metrics
      Metrics.counter(
              "event.published",
              "class",
              event.getClass().getSimpleName(),
              "type",
              event.getType().type)
          .increment();

    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }

  @Override
  public EventService.ReadResponse read() throws AnchorException {
    if (consumer == null) {
      consumer = createConsumer();
      consumer.subscribe(java.util.Collections.singletonList(topic));
    }

    ConsumerRecords<String, AnchorEvent> consumerRecords =
        consumer.poll(Duration.ofSeconds(kafkaConfig.getPollTimeoutSeconds()));
    ArrayList<AnchorEvent> events = new ArrayList<>(consumerRecords.count());
    if (consumerRecords.isEmpty()) {
      Log.debugF("Received {} Kafka records", consumerRecords.count());
    } else {
      Log.infoF("Received {} Kafka records", consumerRecords.count());
      for (ConsumerRecord<String, AnchorEvent> record : consumerRecords) {
        events.add(record.value());
      }
      // TOOD: emit metrics here.
    }
    return new KafkaReadResponse(events);
  }

  @AllArgsConstructor
  public class KafkaReadResponse implements EventService.ReadResponse {
    private final List<AnchorEvent> events;

    @Override
    public List<AnchorEvent> getEvents() {
      return events;
    }
  }

  @Override
  public void ack(EventService.ReadResponse readResponse) throws AnchorException {
    if (consumer != null) {
      consumer.commitSync();
    }
  }

  @Override
  public void close() throws AnchorException {
    if (producer != null) {
      producer.close();
    }
    if (consumer != null) {
      consumer.close();
    }
  }

  @Override
  public String getSessionName() {
    return sessionName;
  }

  private Producer<String, AnchorEvent> createProducer() {
    Log.debugF("kafkaConfig: {}", kafkaConfig);

    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
    props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    if (!isEmpty(kafkaConfig.getClientId())) {
      props.put(CLIENT_ID_CONFIG, kafkaConfig.getClientId());
    }
    props.put(RETRIES_CONFIG, kafkaConfig.getRetries());
    props.put(LINGER_MS_CONFIG, kafkaConfig.getLingerMs());
    props.put(BATCH_SIZE_CONFIG, kafkaConfig.getBatchSize());
    // reconnect back-off is 1 second
    props.put(RECONNECT_BACKOFF_MS_CONFIG, "1000");
    // maximum reconnect back-off is 10 seconds
    props.put(RECONNECT_BACKOFF_MAX_MS_CONFIG, "10000");

    return new KafkaProducer<>(props);
  }

  Consumer<String, AnchorEvent> createConsumer() {
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
    if (!isEmpty(kafkaConfig.getClientId())) {
      props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConfig.getClientId());
    }
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group-" + sessionName);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    return new KafkaConsumer<>(props);
  }
}
