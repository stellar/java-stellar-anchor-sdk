package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.event.EventConfig;
import org.stellar.anchor.config.event.PublisherConfig;
import org.stellar.anchor.platform.configurator.ConfigMap;
import org.stellar.anchor.platform.configurator.PlatformConfigManager;

@Data
public class PropertyEventConfig implements EventConfig, Validator {
  private boolean enabled = false;
  private PropertyPublisherConfig publisher;
  private Map<String, String> eventTypeToQueue = new HashMap<>();

  public PropertyEventConfig() {
    ConfigMap configMap = PlatformConfigManager.getInstance().getConfigMap();
    if (configMap != null) {
      eventTypeToQueue.put(
          "quote_created", configMap.getString("events.event_type_to_queue.quote_created"));
      eventTypeToQueue.put(
          "transaction_created",
          configMap.getString("events.event_type_to_queue.transaction_created"));
      eventTypeToQueue.put(
          "transaction_status_changed",
          configMap.getString("events.transaction_status_changed.quote_created"));
      eventTypeToQueue.put(
          "transaction_error", configMap.getString("events.event_type_to_queue.transaction_error"));
    }
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyEventConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertyEventConfig config = (PropertyEventConfig) target;
    if (!config.isEnabled()) {
      return;
    }

    validateConfig(config, errors);

    PublisherConfig publisherConfig = config.getPublisher();
    String publisherType = publisherConfig.getType();
    switch (publisherType) {
      case "msk":
        validateMsk(config, errors);
        break;
        // continue to the kafka case. DO NOT break
      case "kafka":
        validateKafka(config, errors);
        break;
      case "sqs":
        validateSqs(config, errors);
        break;
      default:
        errors.rejectValue(
            "publisher.type",
            "invalidType-publisher-type",
            "events.publisher.type must be one of 'kafka', 'sqs', or msk");
    }
  }

  void validateConfig(EventConfig config, Errors errors) {
    if (config.getPublisher() == null)
      errors.rejectValue(
          "publisher.type",
          "publisher-type-empty",
          "events.publisher.type is not defined. Please specify the type: KAFKA, SQS, or MSK");
    if (isEmpty(config.getEventTypeToQueue().get("quote_created"))) {
      errors.rejectValue(
          "publisher.event_type_to_queue.quote_created",
          "publisher-event-type-to-queue-quote-created-empty",
          "events.publisher.event_type_to_queue.quote_created is not defined. Please specify the queue to publish the QUOTE_CREATED event");
    }
    if (isEmpty(config.getEventTypeToQueue().get("transaction_created"))) {
      errors.rejectValue(
          "publisher.event_type_to_queue.transaction_created",
          "publisher-event-type-to-transaction_created-empty",
          "events.publisher.event_type_to_queue.transaction_created is not defined. Please specify the queue to publish the QUOTE_CREATED event");
    }
    if (isEmpty(config.getEventTypeToQueue().get("transaction_status_changed"))) {
      errors.rejectValue(
          "publisher.event_type_to_queue.transaction_status_changed",
          "publisher-event-type-to-queue-transaction_status_changed-empty",
          "events.publisher.event_type_to_queue.transaction_status_changed is not defined. Please specify the queue to publish the QUOTE_CREATED event");
    }
    if (isEmpty(config.getEventTypeToQueue().get("transaction_error"))) {
      errors.rejectValue(
          "publisher.event_type_to_queue.transaction_error",
          "publisher-event-type-to-queue-transaction_error-empty",
          "events.publisher.event_type_to_queue.transaction_error is not defined. Please specify the queue to publish the QUOTE_CREATED event");
    }
  }

  void validateSqs(PropertyEventConfig config, Errors errors) {
    if (isEmpty(config.getPublisher().getSqs().awsRegion)) {
      errors.rejectValue(
          "publisher.sqs.awsRegion",
          "sqs-aws-region-empty",
          "events.publisher.sqs.aws_region must be defined");
    }
  }

  void validateKafka(PropertyEventConfig config, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "publisher.kafka.bootstrapServer", "kafka-bootstrap-server-empty");
    if (config.publisher.kafka.retries < 0) {
      errors.rejectValue(
          "publisher.kafka.retries",
          "kafka-retries-invalid",
          "events.publisher.kafka.retries must be greater than 0");
    }

    if (config.publisher.kafka.lingerMs < 0) {
      errors.rejectValue(
          "publisher.kafka.lingerMs",
          "kafka-linger-ms-invalid",
          "events.publisher.kafka.linger_ms must be greater than 0");
    }

    if (config.publisher.kafka.batchSize < 0) {
      errors.rejectValue(
          "publisher.kafka.batchSize",
          "kafka-batch-size-invalid",
          "events.publisher.kafka.batch_size must be greater than 0");
    }
  }

  void validateMsk(PropertyEventConfig config, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "publisher.msk.bootstrapServer", "msk-bootstrap-server-empty");

    if (config.publisher.msk.retries < 0) {
      errors.rejectValue(
          "publisher.msk.retries",
          "msk-retries-invalid",
          "events.publisher.msk.retries must be greater than 0");
    }

    if (config.publisher.msk.lingerMs < 0) {
      errors.rejectValue(
          "publisher.msk.lingerMs",
          "msk-linger-ms-invalid",
          "events.publisher.msk.linger_ms must be greater than 0");
    }

    if (config.publisher.msk.batchSize < 0) {
      errors.rejectValue(
          "publisher.msk.batchSize",
          "msk-batch-size-invalid",
          "events.publisher.msk.batch_size must be greater than 0");
    }
  }
}
