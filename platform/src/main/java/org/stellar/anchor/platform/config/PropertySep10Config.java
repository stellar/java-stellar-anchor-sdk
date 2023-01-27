package org.stellar.anchor.platform.config;

import static java.lang.String.format;
import static org.stellar.anchor.util.StringHelper.isEmpty;
import static org.stellar.anchor.util.StringHelper.isNotEmpty;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.util.ListHelper;
import org.stellar.anchor.util.NetUtil;
import org.stellar.sdk.KeyPair;

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

  public PropertySep10Config(SecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep10Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertySep10Config config = (PropertySep10Config) target;

    if (config.getEnabled()) {
      validateConfig(config, errors);
      validateClientAttribution(config, errors);
      validateOmnibusAccounts(config, errors);
    }
  }

  void validateConfig(Sep10Config config, Errors errors) {
    if (isEmpty(secretConfig.getSep10SigningSeed())) {
      errors.rejectValue(
          null,
          "sep10-signing-seed-empty",
          "Please set environment variable SECRET_SEP10_SIGNING_SEED");
    }

    if (isNotEmpty(secretConfig.getSep10SigningSeed())) {
      try {
        KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
      } catch (Throwable ex) {
        errors.rejectValue(
            null,
            "sep10-signing-seed-invalid",
            "The signing seed of SECRET_SEP10_SIGNING_SEED is invalid");
      }
    }

    if (isEmpty(secretConfig.getSep10JwtSecretKey())) {
      errors.rejectValue(
          null,
          "sep10-jwt-secret-empty",
          "Please set environment variable SECRET_SEP10_JWT_SECRET");
    }

    if (isNotEmpty(config.getHomeDomain())) {
      if (!NetUtil.isServerPortValid(config.getHomeDomain())) {
        errors.rejectValue(
            "homeDomain",
            "sep10-home-domain-invalid",
            "The sep10.home_domain does not have valid format.");
      }
    }

    if (config.getAuthTimeout() <= 0) {
      errors.rejectValue(
          "authTimeout",
          "sep10-auth-timeout-invalid",
          "The sep10.auth_timeout must be greater than 0");
    }

    if (config.getJwtTimeout() <= 0) {
      errors.rejectValue(
          "jwtTimeout",
          "sep10-jwt-timeout-invalid",
          "The sep10.jwt_timeout must be greater than 0");
    }
  }

  void validateClientAttribution(PropertySep10Config config, Errors errors) {
    if (config.clientAttributionRequired) {
      if (ListHelper.isEmpty(config.clientAttributionAllowList)
          && ListHelper.isEmpty(config.clientAttributionDenyList)) {
        errors.reject(
            "sep10-client-attribution-lists-empty",
            "One of sep10.client_attribution_allow_list and sep10.client_attribution_deny_list must NOT be empty while the sep10.client_attribution_required is set to true");
      }
      if (!ListHelper.isEmpty(config.clientAttributionAllowList)
          && !ListHelper.isEmpty(config.clientAttributionDenyList)) {
        errors.reject(
            "sep10-client-attribution-lists-conflict",
            "Only one of sep10.client_attribution_allow_list and sep10.client_attribution_deny_list can be defined while the sep10.client_attribution_required is set to true");
      }

      if (!ListHelper.isEmpty(config.clientAttributionAllowList)) {
        for (String clientDomain : config.clientAttributionAllowList) {
          if (!NetUtil.isServerPortValid(clientDomain)) {
            errors.rejectValue(
                "clientAttributionAllowList",
                "sep10-client_attribution_allow_list_invalid",
                format("%s is not a valid value for client domain.", clientDomain));
          }
        }
      }

      if (!ListHelper.isEmpty(config.clientAttributionDenyList)) {
        for (String clientDomain : config.clientAttributionDenyList) {
          if (!NetUtil.isServerPortValid(clientDomain)) {
            errors.rejectValue(
                "clientAttributionDenyList",
                "sep10-client_attribution_deny_list_invalid",
                format("%s is not a valid value for client domain.", clientDomain));
          }
        }
      }
    } else {
      if (!ListHelper.isEmpty(config.clientAttributionAllowList)) {
        errors.rejectValue(
            "clientAttributionAllowList",
            "sep10-client-attribution-allow-list-not-empty",
            "sep10.client_attribution_allow_list is not not empty while the sep10.client_attribution_required is set to false");
      }
      if (!ListHelper.isEmpty(config.clientAttributionDenyList)) {
        errors.rejectValue(
            "clientAttributionDenyList",
            "sep10-client-attribution-deny-list-not-empty",
            "sep10.client_attrbituion_deny_list is not not empty while the sep10.client_attribution_required is set to false");
      }
    }
  }

  void validateOmnibusAccounts(Sep10Config config, Errors errors) {
    for (String account : config.getOmnibusAccountList()) {
      try {
        if (account != null) KeyPair.fromAccountId(account);
      } catch (Throwable ex) {
        errors.rejectValue(
            "omnibusAccountList",
            "sep10-omnibus-account-not-valid",
            format("Invalid omnibus account:%s in sep10.omnibus_account_list", account));
      }
    }

    if (config.isRequireKnownOmnibusAccount()
        && ListHelper.isEmpty(config.getOmnibusAccountList())) {
      errors.rejectValue(
          "omnibusAccountList",
          "sep10-omnibus-account-list-empty",
          "sep10.omnibus_account_list is empty while sep10.require_known_omnibus_account is set to true");
    }
  }
}
