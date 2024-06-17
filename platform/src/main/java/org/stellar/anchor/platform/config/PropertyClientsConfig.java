package org.stellar.anchor.platform.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.stellar.anchor.client.DefaultClientService.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.error;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.client.CustodialClientConfig;
import org.stellar.anchor.client.NonCustodialClientConfig;
import org.stellar.anchor.config.ClientsConfig;

@Data
public class PropertyClientsConfig implements ClientsConfig, Validator {
  String type;
  String value;
  List<CustodialClientConfig> custodial = emptyList();
  List<NonCustodialClientConfig> noncustodial = emptyList();

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return ClientsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    if (type.equals(CLIENTS_CONFIG_TYPE_FILE)) {
      // validate file type
      Map<String, Object> map = emptyMap();
      try {
        map = parseFileToMap(this.getValue());
      } catch (InvalidConfigException e) {
        error("Error loading clients file", e);
        errors.reject(
            "clients-file-not-valid", "Cannot read from clients file: " + this.getValue());
      }
      // validate file content
      validateCustodialClients(getCustodialClientsFromMap(map), errors);
      validateNonCustodialClients(getNonCustodialClientsFromMap(map), errors);
    } else {
      // validate inline config
      validateCustodialClients(custodial, errors);
      validateNonCustodialClients(noncustodial, errors);
    }
  }

  void validateCustodialClients(List<CustodialClientConfig> clients, Errors errors) {
    for (CustodialClientConfig client : clients) {
      debugF("Validating custodial client {}", client);
      if (client.getSigningKeys() == null || client.getSigningKeys().isEmpty()) {
        errors.reject(
            "invalid-custodial-client-config",
            String.format(
                "Custodial client %s must have at least one signing key", client.getName()));
      }
    }
  }

  void validateNonCustodialClients(List<NonCustodialClientConfig> clients, Errors errors) {
    for (NonCustodialClientConfig client : clients) {
      debugF("Validating noncustodial client {}", client);
      if (client.getDomains() == null || client.getDomains().isEmpty()) {
        errors.reject(
            "invalid-noncustodial-client-config",
            String.format(
                "NonCustodial client %s must have at least one domain", client.getName()));
      }
    }
  }
}
