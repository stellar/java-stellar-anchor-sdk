package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.event.QueueConfig.*;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.event.EventConfig;

@Data
public class PropertyEventConfig implements EventConfig, Validator {
  private boolean enabled = false;
  private PropertyQueueConfig queue;

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

    switch (config.getQueue().getType()) {
      case MSK:
        validateMsk(config, errors);
        break;
      case KAFKA:
        validateKafka(config, errors);
        break;
      case SQS:
        validateSqs(config, errors);
        break;
      default:
        errors.rejectValue(
            "queue.type",
            "invalidType-publisher-type",
            "events.publisher.type must be one of 'KAFKA', 'SQS', or MSK");
    }
  }

  void validateConfig(EventConfig config, Errors errors) {
    if (config.getQueue() == null)
      errors.rejectValue(
          "queue.type",
          "publisher-type-empty",
          "events.publisher.type is not defined. Please specify the type: KAFKA, SQS, or MSK");
  }

  void validateSqs(PropertyEventConfig config, Errors errors) {
    if (isEmpty(config.getQueue().getSqs().awsRegion)) {
      errors.rejectValue(
          "queue.sqs.awsRegion",
          "sqs-aws-region-empty",
          "events.publisher.sqs.aws_region must be defined");
    }
  }

  void validateKafka(PropertyEventConfig config, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "queue.kafka.bootstrapServer", "kafka-bootstrap-server-empty");
    if (config.queue.kafka.retries < 0) {
      errors.rejectValue(
          "queue.kafka.retries",
          "kafka-retries-invalid",
          "events.queue.kafka.retries must be greater than 0");
    }

    if (config.queue.kafka.lingerMs < 0) {
      errors.rejectValue(
          "queue.kafka.lingerMs",
          "kafka-linger-ms-invalid",
          "events.queue.kafka.linger_ms must be greater than 0");
    }

    if (config.queue.kafka.batchSize < 0) {
      errors.rejectValue(
          "queue.kafka.batchSize",
          "kafka-batch-size-invalid",
          "events.queue.kafka.batch_size must be greater than 0");
    }
  }

  void validateMsk(PropertyEventConfig config, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, "queue.msk.bootstrapServer", "msk-bootstrap-server-empty");

    if (config.queue.msk.retries < 0) {
      errors.rejectValue(
          "queue.msk.retries",
          "msk-retries-invalid",
          "events.publisher.msk.retries must be greater than 0");
    }

    if (config.queue.msk.lingerMs < 0) {
      errors.rejectValue(
          "queue.msk.lingerMs",
          "msk-linger-ms-invalid",
          "events.publisher.msk.linger_ms must be greater than 0");
    }

    if (config.queue.msk.batchSize < 0) {
      errors.rejectValue(
          "queue.msk.batchSize",
          "msk-batch-size-invalid",
          "events.publisher.msk.batch_size must be greater than 0");
    }
  }
}
