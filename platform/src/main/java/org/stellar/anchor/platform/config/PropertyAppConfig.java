package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.util.UrlValidationUtil;

@Data
public class PropertyAppConfig implements AppConfig, Validator {
  @Value("${stellar_network.network_passphrase}")
  private String stellarNetworkPassphrase;

  @Value("${host_url}")
  private String hostUrl;

  @Value("${stellar_network.horizon_url}")
  private String horizonUrl;

  private List<String> languages;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return AppConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, Errors errors) {
    AppConfig config = (AppConfig) target;

    ValidationUtils.rejectIfEmpty(
        errors, "stellarNetworkPassphrase", "empty-stellarNetworkPassphrase");
    ValidationUtils.rejectIfEmpty(errors, "hostUrl", "empty-hostUrl");
    ValidationUtils.rejectIfEmpty(errors, "horizonUrl", "empty-horizonUrl");
    UrlValidationUtil.rejectIfMalformed(config.getHostUrl(), "hostUrl", errors);
    UrlValidationUtil.rejectIfMalformed(config.getHorizonUrl(), "horizonUrl", errors);
  }
}
