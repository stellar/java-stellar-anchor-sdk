package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;

@Data
public class PropertySep10Config implements Sep10Config, Validator {
  private Boolean enabled;
  private String homeDomain;
  private boolean clientAttributionRequired = false;
  private Integer authTimeout = 900;
  private Integer jwtTimeout = 86400;
  private List<String> clientAttributionDenyList;
  private List<String> clientAttributionAllowList;
  private List<String> omnibusAccountList;
  private boolean requireKnownOmnibusAccount;
  private SecretConfig secretConfig;
  private JwtService jwtService;

  public PropertySep10Config(SecretConfig secretConfig, JwtService jwtService) {
    this.secretConfig = secretConfig;
    this.jwtService = jwtService;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep10Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    Sep10Config config = (Sep10Config) target;

    if (config.getEnabled()) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "homeDomain", "empty-homeDomain");
      if (isEmpty(secretConfig.getSep10SigningSeed())) {
        errors.rejectValue(
            null,
            "empty-sep10SigningSeed",
            "Please set environment variable secret.sep10.signing_seed");
      }
      if (isEmpty(secretConfig.getSep10JwtSecretKey()) || isEmpty(jwtService.getJwtKey())) {
        errors.rejectValue(
            null,
            "empty-sep10JwtSecret",
            "Please set environment variable secret.sep10.jwt_secret");
      }
    }
  }
}
