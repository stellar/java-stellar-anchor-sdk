package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.stellar.anchor.config.EventTypeToQueueConfig;
import org.stellar.anchor.config.PublisherConfig;

@Data
public class PropertyPublisherConfig implements PublisherConfig {
  private String bootstrapServer;
  private Boolean useSingleQueue;

  private Boolean useIAM;
  private String region;
  private String accessKey;
  private String secretKey;

  private Map<String, String> eventTypeToQueue;

  public PropertyPublisherConfig(EventTypeToQueueConfig eventTypeToQueueConfig) {
    this.eventTypeToQueue = eventTypeToQueueConfig.getEventTypeToQueueMap();
  }

  @Override
  public boolean isUseSingleQueue() {
    return useSingleQueue;
  }

  @Override
  public boolean isUseIAM() {
    return useIAM;
  }

  public BindException validate(String publisherType) {
    BindException errors = new BindException(this, "publisherConfig");
    switch (publisherType) {
      case "kafka":
        ValidationUtils.rejectIfEmptyOrWhitespace(
            errors, "bootstrapServer", "empty-bootstrapServer");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "useSingleQueue", "empty-useSingleQueue");
        break;
      case "sqs":
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "region", "empty-region");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "accessKey", "empty-accessKey");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "secretKey", "empty-secretKey");
        break;
      default:
        errors.rejectValue(
            "publisherType", "invalidType-publisherType", "publisherType set to unknown type");
    }
    return errors;
  }
}
