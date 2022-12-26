package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.util.NetUtil;

@Getter
@Setter
public class PropertySep24Config implements Sep24Config, Validator {
  boolean enabled;
  int interactiveJwtExpiration;
  InteractiveUrlConfig interactiveUrl;
  MoreInfoUrlConfig moreInfoUrl;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class InteractiveUrlConfig {
    String type;
    SimpleInteractiveUrlConfig simple;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SimpleInteractiveUrlConfig {
    String baseUrl;
    List<String> txnFields;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MoreInfoUrlConfig {
    String type;
    SimpleMoreInfoUrlConfig simple;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SimpleMoreInfoUrlConfig {
    String baseUrl;
    List<String> txnFields;
    int jwtExpiration;
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

    if (config.interactiveUrl == null) {
      errors.rejectValue(
          "interactiveUrl",
          "sep24-interactive-url-invalid",
          "sep24.interactive_url is not defined.");
    } else {
      if ("simple".equals(config.interactiveUrl.getType())) {
        if (config.interactiveUrl.getSimple() == null) {
          errors.rejectValue(
              "interactiveUrl",
              "sep24-interactive-url-simple-not-defined",
              "sep24.interactive_url.simple is not defined.");
        }
        if (!NetUtil.isUrlValid(config.interactiveUrl.simple.baseUrl)) {
          errors.rejectValue(
              "interactiveUrl",
              "sep24-interactive-url-simple-base-url-not-valid",
              String.format(
                  "sep24.interactive_url.simple.base_url:[%s] is not a valid URL.",
                  config.interactiveUrl.simple.baseUrl));
        }
      } else {
        errors.rejectValue(
            "interactiveUrl",
            "sep24-interactive-url-invalid-type",
            String.format(
                "sep24.interactive_url.type:[%s] is not supported.",
                config.interactiveUrl.getType()));
      }
    }
  }
}
