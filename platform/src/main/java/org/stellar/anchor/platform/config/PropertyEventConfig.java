package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.event.EventConfig;

import java.util.Map;

@Data
//  public class PropertyEventConfig implements EventConfig, Validator {
public class PropertyEventConfig implements EventConfig {
  private boolean enabled = false;
  private PropertyPublisherConfig publisher;
  private Map<String, String> eventTypeToQueue;

  //  @Override
  //  public boolean supports(Class<?> clazz) {
  //    return PropertyEventConfig.class.isAssignableFrom(clazz);
  //  }

  //  @Override
  //  public void validate(Object target, Errors errors) {
  //    EventConfig config = (EventConfig) target;
  //    if (!config.isEnabled()) {
  //      return;
  //    }
  //
  //    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publisherType", "");
  //
  //    BindException validation = publisherConfig.validate(config.getPublisherType());
  //    if (validation.hasErrors()) {
  //      String errorString = validation.getAllErrors().get(0).toString();
  //      errors.rejectValue(
  //          "publisherConfig",
  //          "badPublisherConfig",
  //          String.format("event publisher not properly configured: %s", errorString));
  //    }
  //  }
}
