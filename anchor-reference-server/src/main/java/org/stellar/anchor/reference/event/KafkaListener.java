package org.stellar.anchor.reference.event;

import static org.stellar.anchor.api.platform.HealthCheckStatus.GREEN;
import static org.stellar.anchor.api.platform.HealthCheckStatus.RED;

import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.stellar.anchor.api.platform.HealthCheckResult;
import org.stellar.anchor.api.platform.HealthCheckStatus;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.reference.config.KafkaListenerSettings;
import org.stellar.anchor.util.Log;

public class KafkaListener extends AbstractEventListener implements HealthCheckable {
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

  Consumer<String, AnchorEvent> createKafkaConsumer() {
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaListenerSettings.getBootStrapServer());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group_one1");
    // props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // start reading from the earliest available message
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    if (kafkaListenerSettings.isUseIAM()) {
      props.put("security.protocol", "SASL_SSL");
      props.put("sasl.mechanism", "AWS_MSK_IAM");
      props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
      props.put(
          "sasl.client.callback.handler.class",
          "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
    }

    return new KafkaConsumer<>(props);
  }

  public void listen() {
    Log.info("Kafka event consumer server started ");

    Consumer<String, AnchorEvent> consumer = createKafkaConsumer();

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

  @Override
  public int compareTo(@NotNull HealthCheckable other) {
    return other.getName().compareTo(other.getName());
  }

  @Override
  public String getName() {
    return "kafka_listener";
  }

  @Override
  public List<String> getTags() {
    return List.of("all", "kafka");
  }

  @Override
  public HealthCheckResult check() {
    HealthCheckStatus status = GREEN;
    if (executor.isTerminated() || executor.isShutdown()) {
      status = RED;
    }

    boolean kafkaAvailable = validateKafka();
    if (!kafkaAvailable) {
      status = RED;
    }

    return KafkaHealthCheckResult.builder()
        .name(getName())
        .status(status)
        .kafkaAvailable(kafkaAvailable)
        .running(!executor.isTerminated())
        .build();
  }

  boolean validateKafka() {
    try (Consumer<String, AnchorEvent> csm = createKafkaConsumer()) {
      csm.listTopics();
      return true;
    } catch (Throwable throwable) {
      return false;
    }
  }
}

@Data
@Builder
class KafkaHealthCheckResult implements HealthCheckResult {
  transient String name;

  List<HealthCheckStatus> statuses = List.of(GREEN, RED);

  HealthCheckStatus status;

  boolean running;

  @SerializedName("kafka_available")
  boolean kafkaAvailable;

  @Override
  public String name() {
    return name;
  }
}
