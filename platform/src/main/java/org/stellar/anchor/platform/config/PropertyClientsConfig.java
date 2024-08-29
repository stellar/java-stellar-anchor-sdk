package org.stellar.anchor.platform.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.stellar.anchor.client.DefaultClientService.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.error;

import com.google.common.collect.ImmutableMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.client.ClientConfig;
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
      validateCallbackUrls(List.of(client), errors);
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
      validateCallbackUrls(List.of(client), errors);
    }
  }

  void validateCallbackUrls(List<ClientConfig> clients, Errors errors) {
    for (ClientConfig client : clients) {
      debugF("Validating client {}", client);
      ImmutableMap.of(
              "callback_url",
              Optional.ofNullable(client.getCallbackUrl()).orElse(""),
              "callback_url_sep6",
              Optional.ofNullable(client.getCallbackUrls())
                  .map(ClientConfig.CallbackUrls::getSep6)
                  .orElse(""),
              "callback_url_sep24",
              Optional.ofNullable(client.getCallbackUrls())
                  .map(ClientConfig.CallbackUrls::getSep24)
                  .orElse(""),
              "callback_url_sep31",
              Optional.ofNullable(client.getCallbackUrls())
                  .map(ClientConfig.CallbackUrls::getSep31)
                  .orElse(""),
              "callback_url_sep12",
              Optional.ofNullable(client.getCallbackUrls())
                  .map(ClientConfig.CallbackUrls::getSep12)
                  .orElse(""))
          .forEach(
              (key, value) -> {
                if (!isEmpty(value)) {
                  try {
                    new URL(value);
                  } catch (MalformedURLException e) {
                    errors.reject("client-invalid-" + key, "The client." + key + " is invalid");
                  }
                }
              });
    }
  }
}
