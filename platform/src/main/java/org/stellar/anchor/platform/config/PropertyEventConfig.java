package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.event.EventConfig;
import org.stellar.anchor.config.event.PublisherConfig;

@Data
public class PropertyEventConfig implements EventConfig, Validator {
  private boolean enabled = false;
  private PropertyPublisherConfig publisher;
  private Map<String, String> eventTypeToQueue;

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

    //    validateConfig(config, errors);

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
    // Validate publisher type
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publisher.type", "publisher-type-empty");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.event_type_to_queue.quote_created",
        "publisher-event-type-to-queue-quote-created-empty",
        "events.publisher.event_type_to_queue.quote_created is not defined. Please specify the queue to publish the QUOTE_CREATED event");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.event_type_to_queue.transaction_created",
        "publisher-event-type-to-queue-transaction-created-empty",
        "events.publisher.event_type_to_queue.transaction_created is not defined. Please specify the queue to publish the TRANSACTION_CREATED event");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.event_type_to_queue.transaction_status_changed",
        "publisher-event-type-to-queue-transaction-status-changed-empty",
        "events.publisher.event_type_to_queue.transaction_status_changed is not defined. Please specify the queue to publish the TRANSACTION_STATUS_CHANGED event");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.event_type_to_queue.transaction_error",
        "publisher-event-type-to-queue-transaction-error-empty",
        "events.publisher.event_type_to_queue.transaction_error is not defined. Please specify the queue to publish the TRANSACTION_ERROR event");
  }

  void validateSqs(EventConfig config, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.sqs.useIAM",
        "sqs-use-iam-empty",
        "use_IAM must be defined for SQS publisher");
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "publisher.sqs.awsRegion",
        "sqs-aws-region-empty",
        "aws_region must be defined for SQS publisher");
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
