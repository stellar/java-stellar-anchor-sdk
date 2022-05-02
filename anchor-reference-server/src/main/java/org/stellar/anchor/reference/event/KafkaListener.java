package org.stellar.anchor.reference.event;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.reference.config.KafkaListenerSettings;
import org.stellar.anchor.util.Log;

public class KafkaListener extends AbstractEventListener {
  private final KafkaListenerSettings kafkaListenerSettings;
  private final AnchorEventProcessor processor;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private Consumer<String, AnchorEvent> consumer;

  public KafkaListener(
      KafkaListenerSettings kafkaListenerSettings, AnchorEventProcessor processor) {
    this.kafkaListenerSettings = kafkaListenerSettings;
    this.processor = processor;
    this.executor.submit(this::listen);
  }

  public void listen() {
    Log.info("Kafka event consumer server started ");
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaListenerSettings.getBootStrapServer());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group_one1");
    // props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // start reading from
    // earliest available message
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    Consumer<String, AnchorEvent> consumer = new KafkaConsumer<>(props);
    KafkaListenerSettings.Queues q = kafkaListenerSettings.getEventTypeToQueue();
    consumer.subscribe(
        List.of(
            q.getAll(),
            q.getQuoteCreated(),
            q.getTransactionCreated(),
            q.getTransactionError(),
            q.getTransactionStatusChanged()));
    this.consumer = consumer;

    while (!Thread.interrupted()) {
      try {
        ConsumerRecords<String, AnchorEvent> consumerRecords =
            consumer.poll(Duration.ofSeconds(10));
        Log.info(String.format("Messages received: %s", consumerRecords.count()));
        consumerRecords.forEach(
            record -> {
              String eventClass = record.value().getClass().getSimpleName();
              switch (eventClass) {
                case "QuoteEvent":
                  processor.handleQuoteEvent((QuoteEvent) record.value());
                  break;
                case "TransactionEvent":
                  processor.handleTransactionEvent((TransactionEvent) record.value());
                  break;
                default:
                  Log.debug(
                      "error: anchor_platform_event - invalid message type '%s'%n", eventClass);
              }
            });
      } catch (Exception ex) {
        Log.errorEx(ex);
      }
    }
  }

  public void stop() {
    executor.shutdownNow();
  }

  @PreDestroy
  public void destroy() {
    consumer.close();
    stop();
  }
}
