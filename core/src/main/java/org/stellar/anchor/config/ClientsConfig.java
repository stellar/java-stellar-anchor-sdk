package org.stellar.anchor.config;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface ClientsConfig {
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  class ClientConfig {
    String name;
    ClientType type;
    @Deprecated String signingKey;
    Set<String> signingKeys;
    @Deprecated String domain;
    Set<String> domains;
    String callbackUrl;
    boolean allowAnyDestination = false;
    Set<String> destinationAccounts;
  }

  enum ClientType {
    CUSTODIAL,
    NONCUSTODIAL
  }

  ClientConfig getClientConfigBySigningKey(String signingKey);

  ClientConfig getClientConfigByDomain(String domain);
}
