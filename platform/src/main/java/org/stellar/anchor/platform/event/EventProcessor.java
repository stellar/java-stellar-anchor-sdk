package org.stellar.anchor.platform.event;

import static org.stellar.anchor.platform.config.EventProcessorConfig.*;
import static org.stellar.anchor.util.Log.debugF;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.util.Log;

public class EventProcessor {
  static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public EventProcessor(EventProcessorConfig config) throws InvalidConfigException {
    if (config.getQueue().getType() == QueueType.KAFKA) {
      startKafkaEventProcessor(config.getQueue().getKafka());
    } else {
      throw new InvalidConfigException("Config ");
    }
  }

  private void startKafkaEventProcessor(KafkaConfig kafkaConfig) {
    scheduler.scheduleWithFixedDelay(new KafkaListeningTask(kafkaConfig), 1, 2, TimeUnit.SECONDS);
  }

  public long getProcessorRestartedCount() {
    return ((ScheduledThreadPoolExecutor) EventProcessor.scheduler).getCompletedTaskCount();
  }
}

class KafkaListeningTask implements Runnable {

  private KafkaConfig kafkaConfig;
  private Consumer<String, AnchorEvent> consumer;

  public KafkaListeningTask(KafkaConfig kafkaConfig) {
    this.kafkaConfig = kafkaConfig;
  }

  Consumer<String, AnchorEvent> createKafkaConsumer() {
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootStrapServer());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "EventProcessor");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

    return new KafkaConsumer<>(props);
  }

  @SneakyThrows
  @Override
  public void run() {
    debugF(
        "The EventProcessor Kafka listening task is starting for the {} time.",
        ((ScheduledThreadPoolExecutor) EventProcessor.scheduler).getCompletedTaskCount());

    this.consumer = createKafkaConsumer();

    KafkaTopicMapping q = kafkaConfig.getEventTypeToTopic();
    consumer.subscribe(
        List.of(
            q.getAll(),
            q.getQuoteCreated(),
            q.getTransactionCreated(),
            q.getTransactionError(),
            q.getTransactionStatusUpdated()));

    try {
      ConsumerRecords<String, AnchorEvent> consumerRecords = consumer.poll(Duration.ofSeconds(10));
      Log.info(String.format("Messages received: %s", consumerRecords.count()));
      consumerRecords.forEach(
          record -> {
            AnchorEvent event = record.value();
            switch (event.getType()) {
              case TRANSACTION_CREATED:
              case TRANSACTION_STATUS_CHANGED:
              case TRANSACTION_ERROR:
                handleTransactionEvent(event);
                break;
              case QUOTE_CREATED:
                handleQuoteEvent(event);
                break;
              default:
                Log.debug("error: anchor_platform_event - invalid message type '%s'%n", eventClass);
            }
          });
    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }

  private void handleTransactionEvent(AnchorEvent event) {}

  private void handleQuoteEvent(AnchorEvent event) {}
}
