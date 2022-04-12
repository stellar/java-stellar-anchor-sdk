package org.stellar.anchor.event;

import java.nio.charset.StandardCharsets;
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
import org.springframework.stereotype.Service;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.event.models.AnchorEvent;

@Component
public class EventService {
  final Producer<String, AnchorEvent> producer;
  final Map<String, String> queues;
  final boolean eventsEnabled;
  final boolean useSingleQueue;

  public EventService(EventConfig eventConfig) {
    // TODO: log the event config
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, eventConfig.getKafkaBootstrapServer());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    this.producer = new KafkaProducer<String, AnchorEvent>(props);

    this.queues = eventConfig.getQueues();
    this.eventsEnabled = eventConfig.isEnabled();
    this.useSingleQueue = eventConfig.isUseSingleQueue();
  }

  public void publish(AnchorEvent event) {
    if(!eventsEnabled){
      return;
    }
    try{
      String topic;
      if(useSingleQueue){
        // TODO: use constant instead of 'all'
        topic = queues.get("all");
      } else {
        topic = queues.get(event.getType());
      }
      ProducerRecord<String, AnchorEvent> record = new ProducerRecord<>(topic, event);
      record.headers().add(new RecordHeader("type", event.getType().getBytes()));
      producer.send(record);
    } catch (Exception ex){
      System.out.println(ex.toString());
    }
  }
}
