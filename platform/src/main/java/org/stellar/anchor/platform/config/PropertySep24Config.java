package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep24Config;

@Data
public class PropertySep24Config implements Sep24Config, Validator {
  boolean enabled;
  int interactiveJwtExpiration;
  InteractiveUrlConfig interactiveUrl;

  @Data
  public static class InteractiveUrlConfig {
    String type;
    SimpleInteractiveUrlConfig simple;
  }

  @Data
  public static class SimpleInteractiveUrlConfig {
    String baseUrl;
    List<String> txnFields;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep24Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertySep24Config config = (PropertySep24Config) target;

    if (config.getInteractiveJwtExpiration() <= 0) {
      errors.rejectValue(
          "interactiveJwtExpiration",
          "sep24-interactive-jwt-expiration-invalid",
          String.format(
              "sep24.interactive_jwt_expiration:%s is not valid",
              config.getInteractiveJwtExpiration()));
    }

    //    if (isEmpty(config.getInteractiveUrl())) {
    //      errors.rejectValue(
    //          "interactiveUrl",
    //          "sep24-interactive-url-invalid",
    //          "sep24.interactive_url is not defined.");
    //    } else if (!NetUtil.isUrlValid(config.getInteractiveUrl())) {
    //      errors.rejectValue(
    //          "interactiveUrl",
    //          "sep24-interactive-url-invalid",
    //          String.format("sep24.interactive_url:%s is not valid", config.getInteractiveUrl()));
    //    }

  }
}
