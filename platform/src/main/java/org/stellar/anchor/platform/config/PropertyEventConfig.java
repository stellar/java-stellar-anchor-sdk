package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
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
  public boolean supports(Class<?> clazz) {
    return PropertyEventConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    EventConfig config = (EventConfig) target;
    if (!config.isEnabled()) {
      return;
    }

    // Validate publisher type
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publisher.type", "");

    PublisherConfig publisherConfig = config.getPublisher();
    String publisherType = publisherConfig.getType();
    switch (publisherType) {
      case "msk":
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors,
            "publisher.msk.awsRegion",
            "empty-aws-region",
            "aws_region must be defined for SQS publisher");
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors,
            "publisher.msk.useIAM",
            "empty-use-iam",
            "use_IAM must be defined for SQS publisher");
        // continue to the kafka case. DO NOT break
      case "kafka":
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors, "publisher.kafka.bootstrapServers", "empty-bootstrapServer");
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors,
            "publisher.kafka.retries",
            "empty-retries",
            "retries must be set for KAFKA/MSK publisher");
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors,
            "publisher.kafka.lingerMs",
            "empty-linger-ms",
            "linger_ms must be set for KAFKA/MSK publisher");

        break;
      case "sqs":
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors,
            "publisher.sqs.awsRegion",
            "empty-aws-region",
            "aws_region must be defined for SQS publisher");
        break;
      default:
        errors.rejectValue(
            "publisherType", "invalidType-publisherType", "publisherType set to unknown type");
    }
  }
}
