package org.stellar.anchor.platform.config;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.stellar.anchor.client.DefaultClientService.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.error;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.client.*;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.util.GsonUtils;

@Data
public class PropertyClientsConfig implements ClientsConfig, Validator {
  ClientsConfig.ClientsConfigType type;
  String value;
  List<TempClient> items = new ArrayList<>();
  Gson gson = GsonUtils.getInstance();

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return ClientsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    // If type is FILE, value must be defined
    // if type is INLINE, items can be left empty as this not a required config
    if (this.getType() == ClientsConfigType.FILE && isEmpty(this.getValue())) {
      errors.reject("invalid-no-value-defined", "clients.value is empty. Please define.");
    }

    // Parse the file and validate the contents
    if (type.equals(ClientsConfigType.FILE)) {
      try {
        items = parseFileToList(this.getValue());
      } catch (InvalidConfigException e) {
        error("Error loading clients file", e);
        errors.reject(
            "clients-file-not-valid", "Cannot read from clients file: " + this.getValue());
      }
    }

    // validate custodial client and noncustodial client
    for (TempClient item : items) {
      if (item.getType().equals(ClientConfig.ClientType.CUSTODIAL)) {
        validateCustodialClient(item.toCustodialClient(), errors);
      } else if (item.getType().equals(ClientConfig.ClientType.NONCUSTODIAL)) {
        validateNonCustodialClient(item.toNonCustodialClient(), errors);
      } else {
        errors.reject(
            "invalid-client-type", String.format("Client type %s is invalid", item.getType()));
      }
    }
  }

  void validateCustodialClient(CustodialClient client, Errors errors) {
    debugF("Validating custodial client {}", client);
    if (client.getSigningKeys() == null || client.getSigningKeys().isEmpty()) {
      errors.reject(
          "invalid-custodial-client-config",
          String.format(
              "Custodial client %s must have at least one signing key", client.getName()));
    }
    validateCallbackUrls(client, errors);
  }

  void validateNonCustodialClient(NonCustodialClient client, Errors errors) {
    debugF("Validating noncustodial client {}", client);
    if (client.getDomains() == null || client.getDomains().isEmpty()) {
      errors.reject(
          "invalid-noncustodial-client-config",
          String.format("NonCustodial client %s must have at least one domain", client.getName()));
    }
    validateCallbackUrls(client, errors);
  }

  void validateCallbackUrls(ClientConfig client, Errors errors) {
    debugF("Validating client {}", client);
    ImmutableMap.of(
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
