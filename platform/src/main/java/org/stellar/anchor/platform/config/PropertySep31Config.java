package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE;
import static org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.SELF;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CircleConfig;
import org.stellar.anchor.config.Sep31Config;

@Data
public class PropertySep31Config implements Sep31Config, Validator {
  boolean enabled = false;
  String feeIntegrationEndPoint = "http://localhost:8081";
  PaymentType paymentType = STRICT_SEND;
  DepositInfoGeneratorType depositInfoGeneratorType = SELF;

  CircleConfig circleConfig;

  public PropertySep31Config(CircleConfig circleConfig) {
    this.circleConfig = circleConfig;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep31Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep31Config config = (Sep31Config) target;
    if (config.isEnabled()) {
      if (config.getDepositInfoGeneratorType().equals(CIRCLE)) {
        if (circleConfig.validate().hasErrors()) {
          errors.rejectValue(
              "circleConfig",
              "badConfig-circle",
              "depositInfoGeneratorType set as circle, but circle config not properly configured");
        }
      }
    }
  }
}
