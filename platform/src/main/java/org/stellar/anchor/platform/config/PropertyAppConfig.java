package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.List;
import lombok.Data;
import org.apache.abdera.i18n.rfc4646.Lang;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.util.NetUtil;

@Data
public class PropertyAppConfig implements AppConfig, Validator {
  @Value("${stellar_network.network_passphrase}")
  private String stellarNetworkPassphrase;

  @Value("${stellar_network.horizon_url}")
  private String horizonUrl;

  private List<String> languages;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return AppConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    AppConfig config = (AppConfig) target;

    validateConfig(config, errors);
    validateLanguage(config, errors);
  }

  void validateConfig(AppConfig config, Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "stellarNetworkPassphrase",
        "stellar-network-passphrase-empty",
        "stellar_network.network_passphrase is not defined.");

    if (isEmpty(config.getHorizonUrl())) {
      errors.rejectValue(
          "horizonUrl", "horizon-url-empty", "The stellar_network.horizon_url is not defined.");
    } else {
      if (!NetUtil.isUrlValid(config.getHorizonUrl())) {
        errors.rejectValue(
            "horizonUrl",
            "horizon-url-invalid",
            String.format(
                "The stellar_network.horizon_url:%s is not in valid format.",
                config.getHorizonUrl()));
      }
    }
  }

  void validateLanguage(AppConfig config, Errors errors) {
    if (config.getLanguages() != null) {
      for (String lang : config.getLanguages()) {
        try {
          Lang.parse(lang);
        } catch (IllegalArgumentException iaex) {
          errors.rejectValue(
              "languages",
              "languages-invalid",
              String.format("%s defined in languages is not valid", lang));
        }
      }
    }
  }
}
