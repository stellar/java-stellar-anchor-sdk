package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.PublisherConfig;

@Data
public class PropertyEventConfig implements EventConfig, Validator {
  private boolean enabled = false;
  private String publisherType;

  PublisherConfig publisherConfig;

  public PropertyEventConfig(PublisherConfig kafkaConfig) {
    this.publisherConfig = kafkaConfig;
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

    BindException validation = publisherConfig.validate(config.getPublisherType());
    if (validation.hasErrors()) {
      String errorString = validation.getAllErrors().get(0).toString();
      errors.rejectValue(
          "publisherConfig",
          "badPublisherConfig",
          String.format("event publisher not properly configured: %s", errorString));
    }
  }
}
