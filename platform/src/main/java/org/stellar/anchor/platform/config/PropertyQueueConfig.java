package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.io.IOException;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.event.*;
import org.stellar.anchor.platform.utils.ResourceHelper;

@Data
public class PropertyQueueConfig implements QueueConfig, Validator {
  QueueType type;
  KafkaConfig kafka;
  SqsConfig sqs;
  MskConfig msk;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyQueueConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertyQueueConfig config = (PropertyQueueConfig) target;
    if (config.getType() == null) {
      errors.rejectValue(
          "type",
          "queue-type-empty",
          "queue.type is not defined. Please specify the type: KAFKA, SQS, or MSK");
      return;
    }

    switch (config.getType()) {
      case KAFKA:
        validateKafka(config, errors);
        break;
      case SQS:
        validateSqs(config, errors);
        break;
      case MSK:
        validateMsk(config, errors);
        break;
      default:
        errors.rejectValue(
            "type", "invalidType-queue-type", "queue.type must be one of 'KAFKA', 'SQS', or MSK");
    }
  }

  private void validateMsk(PropertyQueueConfig ignoredConfig, Errors errors) {
    errors.rejectValue(
        "msk",
        "queue-type-msk-not-implemented",
        "MSK is not implemented. Please specify a different queue type.");
  }

  private void validateSqs(PropertyQueueConfig ignoredConfig, Errors errors) {
    errors.rejectValue(
        "sqs",
        "queue-type-sqs-not-implemented",
        "SQS is not implemented. Please specify a different queue type.");
  }

  private void validateKafka(PropertyQueueConfig config, Errors errors) {
    if (config.getKafka() == null) {
      errors.rejectValue(
          "kafka",
          "queue-kafka-empty",
          "queue.kafka is not defined. Please specify the Kafka configuration.");
    } else {
      KafkaConfig kafkaConfig = config.getKafka();
      if (kafkaConfig.getBootstrapServer() == null) {
        errors.rejectValue(
            "kafka.bootstrapServer",
            "kafka-bootstrap-server-empty",
            "queue.kafka.bootstrap_server is not defined. Please specify the Kafka bootstrap server.");
      }
      if (kafkaConfig.getRetries() < 0) {
        errors.rejectValue(
            "kafka.retries",
            "kafka-retries-invalid",
            "queue.kafka.retries must be greater than or equal to 0.");
      }

      if (kafkaConfig.getLingerMs() < 0) {
        errors.rejectValue(
            "kafka.lingerMs",
            "kafka-linger-ms-invalid",
            "queue.kafka.linger_ms must be greater than or equal to 0.");
      }

      if (kafkaConfig.getBatchSize() <= 0) {
        errors.rejectValue(
            "kafka.batchSize",
            "kafka-batch-size-invalid",
            "queue.kafka.batch_size must be greater than 0.");
      }

      if (kafkaConfig.getPollTimeoutSeconds() <= 0) {
        errors.rejectValue(
            "kafka.pollTimeoutSeconds",
            "kafka-poll-timeout-seconds-invalid",
            "queue.kafka.poll_timeout_seconds must be greater than 0.");
      }

      if (kafkaConfig.getSecurityProtocol() != null
          && kafkaConfig.getSecurityProtocol() != KafkaConfig.SecurityProtocol.PLAINTEXT) {
        if (kafkaConfig.getSaslMechanism() == null) {
          errors.rejectValue(
              "kafka.saslMechanism",
              "kafka-sasl-mechanism-empty",
              "queue.kafka.sasl_mechanism must be defined if securityProtocol is not PLAINTEXT.");
        }
      }

      if (kafkaConfig.getSecurityProtocol() == KafkaConfig.SecurityProtocol.SASL_SSL) {
        if (isEmpty(kafkaConfig.getSslKeystoreLocation())) {
          errors.rejectValue(
              "kafka.sslKeystoreLocation",
              "kafka-ssl-keystore-location-empty",
              "queue.kafka.ssl_keystore_location must be defined if securityProtocol is SASL_SSL.");
        } else {
          try {
            ResourceHelper.findFileThenResource(kafkaConfig.getSslKeystoreLocation());
          } catch (IOException e) {
            errors.rejectValue(
                "kafka.sslKeystoreLocation",
                "kafka-ssl-keystore-location-not-found",
                "queue.kafka.ssl_keystore_location file not found.");
          }
        }
        if (isEmpty(kafkaConfig.getSslTruststoreLocation())) {
          errors.rejectValue(
              "kafka.sslTruststoreLocation",
              "kafka-ssl-truststore-location-empty",
              "queue.kafka.ssl_truststore_location must be defined if securityProtocol is SASL_SSL.");
        } else {
          try {
            ResourceHelper.findFileThenResource(kafkaConfig.getSslTruststoreLocation());
          } catch (IOException e) {
            errors.rejectValue(
                "kafka.sslTruststoreLocation",
                "kafka-ssl-truststore-location-not-found",
                "queue.kafka.ssl_truststore_location file not found.");
          }
        }
      }
    }
  }
}
