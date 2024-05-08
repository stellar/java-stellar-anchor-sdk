package org.stellar.anchor.platform.config;

import static io.jsonwebtoken.lang.Collections.setOf;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.ClientsConfig;

@Data
public class PropertyClientsConfig implements ClientsConfig, Validator {
  List<ClientConfig> clients = Lists.newLinkedList();
  Map<String, ClientConfig> clientMap = null;
  Map<String, String> domainToClientNameMap = null;
  Map<String, String> signingKeyToClientNameMap = null;

  @PostConstruct
  public void postConstruct() {
    clients.forEach(
        clientConfig -> {
          if (!isEmpty(clientConfig.getSigningKey())
              && (clientConfig.getSigningKeys() == null
                  || clientConfig.getSigningKeys().isEmpty())) {
            clientConfig.setSigningKeys(setOf(clientConfig.getSigningKey()));
          }
          if (!isEmpty(clientConfig.getDomain())
              && (clientConfig.getDomains() == null || clientConfig.getDomains().isEmpty())) {
            clientConfig.setDomains(setOf(clientConfig.getDomain()));
          }
        });
  }

  @Override
  public ClientsConfig.ClientConfig getClientConfigBySigningKey(String signingKey) {
    if (signingKeyToClientNameMap == null) {
      signingKeyToClientNameMap = Maps.newHashMap();
      clients.forEach(
          clientConfig -> {
            if (clientConfig.getSigningKeys() != null && !clientConfig.getSigningKeys().isEmpty()) {
              for (String key : clientConfig.getSigningKeys()) {
                signingKeyToClientNameMap.put(key, clientConfig.getName());
              }
            }
          });
    }
    return getClientConfigByName(signingKeyToClientNameMap.get(signingKey));
  }

  @Override
  public ClientConfig getClientConfigByDomain(String domain) {
    if (domainToClientNameMap == null) {
      domainToClientNameMap = Maps.newHashMap();
      clients.forEach(
          clientConfig -> {
            if (clientConfig.getDomains() != null && !clientConfig.getDomains().isEmpty()) {
              for (String domainName : clientConfig.getDomains()) {
                domainToClientNameMap.put(domainName, clientConfig.getName());
              }
            }
          });
    }
    return getClientConfigByName(domainToClientNameMap.get(domain));
  }

  public ClientConfig getClientConfigByName(String name) {
    if (clientMap == null) {
      clientMap = Maps.newHashMap();
      clients.forEach(clientConfig -> clientMap.put(clientConfig.getName(), clientConfig));
    }
    return clientMap.get(name);
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyClientsConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PropertyClientsConfig configs = (PropertyClientsConfig) target;
    configs.clients.forEach(clientConfig -> validateClient(clientConfig, errors));
  }

  private void validateClient(ClientConfig clientConfig, Errors errors) {
    debugF("Validating client {}", clientConfig);
    if (isEmpty(clientConfig.getName())) {
      errors.reject("empty-client-name", "The client.name cannot be empty and must be defined");
    }
    if (clientConfig.getType().equals(ClientType.CUSTODIAL)) {
      validateCustodialClient(clientConfig, errors);
    } else {
      validateNonCustodialClient(clientConfig, errors);
    }
  }

  public void validateCustodialClient(ClientConfig clientConfig, Errors errors) {
    if (clientConfig.getSigningKeys() == null || clientConfig.getSigningKeys().isEmpty()) {
      errors.reject(
          "empty-client-signing-keys",
          "The client.signingKeys cannot be empty and must be defined");
    }
    if (!isEmpty(clientConfig.getCallbackUrl())) {
      try {
        new URL(clientConfig.getCallbackUrl());
      } catch (MalformedURLException e) {
        errors.reject("client-invalid-callback_url", "The client.callbackUrl is invalid");
      }
    }
  }

  public void validateNonCustodialClient(ClientConfig clientConfig, Errors errors) {
    if (clientConfig.getDomains() == null || clientConfig.getDomains().isEmpty()) {
      errors.reject(
          "empty-client-domains", "The client.domains cannot be empty and must be defined");
    }

    if (!isEmpty(clientConfig.getCallbackUrl())) {
      try {
        new URL(clientConfig.getCallbackUrl());
      } catch (MalformedURLException e) {
        errors.reject("client-invalid-callback_url", "The client.callbackUrl is invalid");
      }
    }

    if (clientConfig.getDestinationAccounts() != null) {
      errors.reject(
          "destination-accounts-noncustodial",
          "Destination accounts list is not a valid configuration option for a non-custodial client");
    }
  }
}
