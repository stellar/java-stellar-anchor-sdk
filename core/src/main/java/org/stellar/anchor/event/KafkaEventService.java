package org.stellar.anchor.event;

import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.stellar.anchor.config.KafkaConfig;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.util.Log;

@Component
public class KafkaEventService implements EventPublishService {
  final Producer<String, AnchorEvent> producer;
  final Map<String, String> eventTypeToQueue;
  final boolean useSingleQueue;

  public KafkaEventService(KafkaConfig kafkaConfig) {
    // TODO: log the event config
    Properties props = new Properties();
    Log.debug("REECEDEBUG KAFKA '%s",kafkaConfig.getBootstrapServer() );
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    if (kafkaConfig.isUseIAM()) {
      props.put("security.protocol", "SASL_SSL");
      props.put("sasl.mechanism", "AWS_MSK_IAM");
      props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
      props.put(
          "sasl.client.callback.handler.class",
          "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
    }

    this.producer = new KafkaProducer<String, AnchorEvent>(props);

    this.eventTypeToQueue = kafkaConfig.getEventTypeToQueue();
    this.useSingleQueue = kafkaConfig.isUseSingleQueue();
  }

  public void publish(AnchorEvent event) {
    try {
      String topic;
      if (useSingleQueue) {
        topic = eventTypeToQueue.get("all");
      } else {
        topic = eventTypeToQueue.get(event.getType());
      }
      ProducerRecord<String, AnchorEvent> record = new ProducerRecord<>(topic, event);
      record.headers().add(new RecordHeader("type", event.getType().getBytes()));
      producer.send(record);
    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }
}
