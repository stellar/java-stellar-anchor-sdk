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
  private String webAuthDomain;
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
      validateConfig(errors);
      validateClientAttribution(errors);
      validateOmnibusAccounts(errors);
    }
  }

  void validateConfig(Errors errors) {
    if (isEmpty(secretConfig.getSep10SigningSeed())) {
      errors.reject(
          "sep10-signing-seed-empty", "Please set environment variable SECRET_SEP10_SIGNING_SEED");
    }

    if (isNotEmpty(secretConfig.getSep10SigningSeed())) {
      try {
        KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
      } catch (Throwable ex) {
        errors.reject(
            "sep10-signing-seed-invalid",
            "Please set the secret.sep10.signing_seed or SECRET_SEP10_SIGNING_SEED environment variable");
      }
    }

    if (isEmpty(secretConfig.getSep10JwtSecretKey())) {
      errors.reject(
          "sep10-jwt-secret-empty",
          "Please set the secret.sep10.jwt_secret or SECRET_SEP10_JWT_SECRET environment variable");
    }

    if (isNotEmpty(webAuthDomain)) {
      if (!NetUtil.isServerPortValid(webAuthDomain)) {
        errors.rejectValue(
            "homeDomain",
            "sep10-home-domain-invalid",
            "The sep10.home_domain does not have valid format.");
      }
    }

    if (authTimeout <= 0) {
      errors.rejectValue(
          "authTimeout",
          "sep10-auth-timeout-invalid",
          "The sep10.auth_timeout must be greater than 0");
    }

    if (jwtTimeout <= 0) {
      errors.rejectValue(
          "jwtTimeout",
          "sep10-jwt-timeout-invalid",
          "The sep10.jwt_timeout must be greater than 0");
    }
  }

  void validateClientAttribution(Errors errors) {
    if (clientAttributionRequired) {
      if (ListHelper.isEmpty(clientAttributionAllowList)
          && ListHelper.isEmpty(clientAttributionDenyList)) {
        errors.reject(
            "sep10-client-attribution-lists-empty",
            "One of sep10.client_attribution_allow_list and sep10.client_attribution_deny_list must NOT be empty while the sep10.client_attribution_required is set to true");
      }
      if (!ListHelper.isEmpty(clientAttributionAllowList)
          && !ListHelper.isEmpty(clientAttributionDenyList)) {
        errors.reject(
            "sep10-client-attribution-lists-conflict",
            "Only one of sep10.client_attribution_allow_list and sep10.client_attribution_deny_list can be defined while the sep10.client_attribution_required is set to true");
      }

      if (!ListHelper.isEmpty(clientAttributionAllowList)) {
        for (String clientDomain : clientAttributionAllowList) {
          if (!NetUtil.isServerPortValid(clientDomain)) {
            errors.rejectValue(
                "clientAttributionAllowList",
                "sep10-client_attribution_allow_list_invalid",
                format("%s is not a valid value for client domain.", clientDomain));
          }
        }
      }

      if (!ListHelper.isEmpty(clientAttributionDenyList)) {
        for (String clientDomain : clientAttributionDenyList) {
          if (!NetUtil.isServerPortValid(clientDomain)) {
            errors.rejectValue(
                "clientAttributionDenyList",
                "sep10-client_attribution_deny_list_invalid",
                format("%s is not a valid value for client domain.", clientDomain));
          }
        }
      }
    } else {
      if (!ListHelper.isEmpty(clientAttributionAllowList)) {
        errors.rejectValue(
            "clientAttributionAllowList",
            "sep10-client-attribution-allow-list-not-empty",
            "sep10.client_attribution_allow_list is not not empty while the sep10.client_attribution_required is set to false");
      }
      if (!ListHelper.isEmpty(clientAttributionDenyList)) {
        errors.rejectValue(
            "clientAttributionDenyList",
            "sep10-client-attribution-deny-list-not-empty",
            "sep10.client_attrbituion_deny_list is not not empty while the sep10.client_attribution_required is set to false");
      }
    }
  }

  void validateOmnibusAccounts(Errors errors) {
    for (String account : omnibusAccountList) {
      try {
        if (account != null) KeyPair.fromAccountId(account);
      } catch (Throwable ex) {
        errors.rejectValue(
            "omnibusAccountList",
            "sep10-omnibus-account-not-valid",
            format("Invalid omnibus account:%s in sep10.omnibus_account_list", account));
      }
    }

    if (requireKnownOmnibusAccount && ListHelper.isEmpty(omnibusAccountList)) {
      errors.rejectValue(
          "omnibusAccountList",
          "sep10-omnibus-account-list-empty",
          "sep10.omnibus_account_list is empty while sep10.require_known_omnibus_account is set to true");
    }
  }
}
