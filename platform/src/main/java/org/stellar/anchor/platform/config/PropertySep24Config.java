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
    String baseUrl;
    int jwtExpiration;
    List<String> txnFields;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MoreInfoUrlConfig {
    String baseUrl;
    int jwtExpiration;
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

    validateInteractiveUrlConfig(config, errors);
    validateMoreInfoUrlConfig(config, errors);
  }

  void validateInteractiveUrlConfig(PropertySep24Config config, Errors errors) {
    if (config.interactiveUrl == null) {
      errors.rejectValue(
          "interactiveUrl",
          "sep24-interactive-url-invalid",
          "sep24.interactive_url is not defined.");
    } else {
      if (!NetUtil.isUrlValid(config.interactiveUrl.baseUrl)) {
        errors.rejectValue(
            "interactiveUrl",
            "sep24-interactive-url-base-url-not-valid",
            String.format(
                "sep24.interactive_url.base_url:[%s] is not a valid URL.",
                config.interactiveUrl.baseUrl));
      }
      if (config.interactiveUrl.jwtExpiration <= 0) {
        errors.rejectValue(
            "moreInfoUrl",
            "sep24-interactive-url-jwt-expiration-not-valid",
            String.format(
                "sep24.interactive_url.jwt_expiration:[%s] must be greater than 0.",
                config.interactiveUrl.jwtExpiration));
      }
    }
  }

  void validateMoreInfoUrlConfig(PropertySep24Config config, Errors errors) {
    if (config.moreInfoUrl == null) {
      errors.rejectValue(
          "moreInfoUrl", "sep24-moreinfo-url-invalid", "sep24.more-info-url is not defined.");
    } else {
      if (!NetUtil.isUrlValid(config.moreInfoUrl.baseUrl)) {
        errors.rejectValue(
            "moreInfoUrl",
            "sep24-more-info-url-base-url-not-valid",
            String.format(
                "sep24.more_info_url.base_url:[%s] is not a valid URL.",
                config.moreInfoUrl.baseUrl));
      }
      if (config.moreInfoUrl.jwtExpiration <= 0) {
        errors.rejectValue(
            "moreInfoUrl",
            "sep24-more-info-url-jwt-expiration-not-valid",
            String.format(
                "sep24.more_info_url.jwt_expiration:[%s] must be greater than 0.",
                config.moreInfoUrl.jwtExpiration));
      }
    }
  }
}
