package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.KafkaConfig;
import org.stellar.anchor.config.SqsConfig;

@Data
public class PropertyEventConfig implements EventConfig, Validator {
  private boolean enabled = false;
  private String publisherType;

  KafkaConfig kafkaConfig;
  SqsConfig sqsConfig;

  public PropertyEventConfig(KafkaConfig kafkaConfig, SqsConfig sqsConfig) {
    this.kafkaConfig = kafkaConfig;
    this.sqsConfig = sqsConfig;
  }

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

    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publisherType", "");

    switch (config.getPublisherType()) {
      case "kafka":
        BindException validation = kafkaConfig.validate();
        if (validation.hasErrors()) {
          String errorString = validation.getAllErrors().get(0).toString();
          errors.rejectValue(
              "kafkaConfig",
              "badConfig-kafka",
              String.format(
                  "publisherType set to kafka, but kafka config not properly configured: %s",
                  errorString));
        }
        break;
      case "sqs":
        validation = sqsConfig.validate();
        if (validation.hasErrors()) {
          String errorString = validation.getAllErrors().get(0).toString();
          errors.rejectValue(
              "sqsConfig",
              "badConfig-sqs",
              String.format(
                  "publisherType set to sqs, but sqs config not properly configured: %s",
                  errorString));
        }
        break;
      default:
        errors.rejectValue(
            "publisherType", "invalidType-publisherType", "publisherType set to unknown type");
    }
  }
}
