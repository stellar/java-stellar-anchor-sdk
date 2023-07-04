package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.sep10.Sep10Helper;

@Data
public class ClientsConfig implements Validator {
  List<ClientConfig> clients;

  @Getter
  public static class ClientConfig {
    ClientType type;
    String signingKey;
    String domain;
    String callbackUrl;
  }

  public enum ClientType {
    CUSTODIAL,
    NONCUSTODIAL
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return ClientsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    ClientsConfig configs = (ClientsConfig) target;
    configs.clients.forEach(clientConfig -> validateClient(clientConfig, errors));
  }

  private void validateClient(ClientConfig clientConfig, Errors errors) {
    if (clientConfig.type.equals(ClientType.CUSTODIAL)) {
      validateCustodialClient(errors);
    } else {
      validateNonCustodialClient(clientConfig, errors);
    }
  }

  void validateCustodialClient(Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "signingKey",
        "empty-client-signing-key",
        "The client.signingKey cannot be empty and must be defined");
  }

  void validateNonCustodialClient(ClientConfig clientConfig, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "domain",
        "empty-client-domain",
        "The client.domain cannot be empty and must be defined");

    if (!isEmpty(clientConfig.signingKey)) {
      try {
        String clientSigningKey = Sep10Helper.fetchSigningKeyFromClientDomain(clientConfig.domain);
        if (!clientConfig.signingKey.equals(clientSigningKey)) {
          errors.rejectValue(
              "signingKey",
              "client-signing-key-does-not-match",
              "The client.signingKey does not matched any valid registered keys");
        }
      } catch (SepException e) {
        errors.rejectValue(
            "signingKey",
            "client-signing-key-toml-read-failure",
            "SIGNING_KEY not present in 'client_domain' TOML or TOML file does not exist");
      }
    }
  }
}
