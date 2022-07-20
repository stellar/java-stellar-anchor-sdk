package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CircleConfig;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.KafkaConfig;
import org.stellar.anchor.config.Sep31Config;
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
    if (config.isEnabled()) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publisherType", "");
      if (config.getPublisherType().equals("kafka")) {
        if (kafkaConfig.validate().hasErrors()) {
          errors.rejectValue(
              "kafka",
              "badConfig-kafka",
              "publisherType set to kafka, but kafka config not properly configured");
        }
      } else if (config.getPublisherType().equals("sqs")) {
        if (sqsConfig.validate().hasErrors()) {
          errors.rejectValue(
              "sqs",
              "badConfig-sqs",
              "publisherType set to sqs, but sqs config not properly configured");
        }
      } else {
        errors.rejectValue(
            "publisherType", "invalidType-publisherType", "publisherType set to unknown type");
      }
    }
  }
}
