package org.stellar.anchor.platform.config;

import java.util.Map;
import lombok.Data;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.stellar.anchor.config.SqsConfig;

@Data
public class PropertySqsConfig implements SqsConfig {
  private String region;
  private Boolean useSingleQueue;
  private Map<String, String> eventTypeToQueue;
  private String accessKey;
  private String secretKey;

  @Override
  public Boolean isUseSingleQueue() {
    return useSingleQueue;
  }

  public BindException validate() {
    BindException errors = new BindException(this, "sqsConfig");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "region", "empty-region");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "accessKey", "empty-accessKey");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "secretKey", "empty-secretKey");
    return errors;
  }
}
