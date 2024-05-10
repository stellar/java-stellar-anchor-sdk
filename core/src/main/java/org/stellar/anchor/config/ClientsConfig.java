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
    @Deprecated String signingKey; // ANCHOR-696
    Set<String> signingKeys;
    @Deprecated String domain; // ANCHOR-696
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
