package org.stellar.anchor.platform.config;

import static java.lang.String.format;
import static org.stellar.anchor.platform.config.ClientsConfig.ClientType.CUSTODIAL;
import static org.stellar.anchor.platform.config.ClientsConfig.ClientType.NONCUSTODIAL;
import static org.stellar.anchor.util.StringHelper.isEmpty;
import static org.stellar.anchor.util.StringHelper.isNotEmpty;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.platform.config.ClientsConfig.ClientConfig;
import org.stellar.anchor.util.ListHelper;
import org.stellar.anchor.util.NetUtil;
import org.stellar.anchor.util.StringHelper;
import org.stellar.sdk.*;

@Data
public class PropertySep10Config implements Sep10Config, Validator {
  private Boolean enabled;
  private String webAuthDomain;
  private String homeDomain;
  private boolean clientAttributionRequired = false;
  private Integer authTimeout = 900;
  private Integer jwtTimeout = 86400;
  private boolean requireKnownOmnibusAccount;
  private AppConfig appConfig;
  private final ClientsConfig clientsConfig;
  private SecretConfig secretConfig;

  public PropertySep10Config(
      AppConfig appConfig, ClientsConfig clientsConfig, SecretConfig secretConfig) {
    this.appConfig = appConfig;
    this.clientsConfig = clientsConfig;
    this.secretConfig = secretConfig;
  }

  @PostConstruct
  public void postConstruct() throws MalformedURLException {
    if (isEmpty(webAuthDomain)) {
      webAuthDomain = homeDomain;
    }
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

    if (isEmpty(homeDomain)) {
      errors.rejectValue(
          "homeDomain", "home-domain-empty", "The sep10.home_domain is not defined.");
    } else {
      try {
        new ManageDataOperation.Builder(String.format("%s %s", homeDomain, "auth"), new byte[64])
            .build();
      } catch (IllegalArgumentException iaex) {
        errors.rejectValue(
            "homeDomain",
            "sep10-home-domain-too-long",
            format(
                "The sep10.home_domain (%s) is longer than the maximum length (64) of a domain. Error=%s",
                homeDomain, iaex));
      }

      if (!NetUtil.isServerPortValid(homeDomain)) {
        errors.rejectValue(
            "homeDomain",
            "sep10-home-domain-invalid",
            "The sep10.home_domain does not have valid format.");
      }
    }

    if (isNotEmpty(webAuthDomain)) {
      try {
        new ManageDataOperation.Builder(webAuthDomain, new byte[64]).build();
      } catch (IllegalArgumentException iaex) {
        errors.rejectValue(
            "webAuthDomain",
            "sep10-web-auth-domain-too-long",
            format(
                "The sep10.web_auth_home_domain (%s) is longer than the maximum length (64) of a domain. Error=%s",
                webAuthDomain, iaex));
      }

      if (!NetUtil.isServerPortValid(webAuthDomain)) {
        errors.rejectValue(
            "webAuthDomain",
            "sep10-web-auth-domain-invalid",
            "The sep10.web_auth_domain does not have valid format.");
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
      if (ListHelper.isEmpty(getClientAttributionAllowList())) {
        errors.reject(
            "sep10-client-attribution-lists-empty",
            "sep10.client_attribution_required is set to true but no NONCUSTODIAL clients are defined in the clients: section of the configuration.");
      }

      if (!ListHelper.isEmpty(getClientAttributionAllowList())) {
        for (String clientDomain : getClientAttributionAllowList()) {
          if (!NetUtil.isServerPortValid(clientDomain)) {
            errors.rejectValue(
                "clientAttributionAllowList",
                "sep10-client_attribution_allow_list_invalid",
                format("%s is not a valid value for client domain.", clientDomain));
          }
        }
      }
    }
  }

  void validateOmnibusAccounts(Errors errors) {
    for (String account : getOmnibusAccountList()) {
      try {
        if (account != null) KeyPair.fromAccountId(account);
      } catch (Throwable ex) {
        errors.rejectValue(
            "omnibusAccountList",
            "sep10-omnibus-account-not-valid",
            format("Invalid omnibus account:%s in sep10.omnibus_account_list", account));
      }
    }

    if (requireKnownOmnibusAccount && ListHelper.isEmpty(getOmnibusAccountList())) {
      errors.rejectValue(
          "omnibusAccountList",
          "sep10-omnibus-account-list-empty",
          "sep10.omnibus_account_list is empty while sep10.require_known_omnibus_account is set to true");
    }
  }

  @Override
  public List<String> getClientAttributionAllowList() {
    return clientsConfig.clients.stream()
        .filter(cfg -> cfg.getType() == NONCUSTODIAL && StringHelper.isNotEmpty(cfg.getDomain()))
        .map(ClientConfig::getDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getOmnibusAccountList() {
    return clientsConfig.clients.stream()
        .filter(cfg -> cfg.getType() == CUSTODIAL && StringHelper.isNotEmpty(cfg.getSigningKey()))
        .map(ClientConfig::getSigningKey)
        .collect(Collectors.toList());
  }
}
