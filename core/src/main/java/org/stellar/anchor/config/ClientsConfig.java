package org.stellar.anchor.config;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface ClientsConfig {
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  class ClientConfig {
    String name;
    ClientType type;
    @Deprecated String signingKey; // ANCHOR-696
    Set<String> signingKeys;
    @Deprecated String domain; // ANCHOR-696
    Set<String> domains;
    String callbackUrl;
    String callbackUrlSep6;
    String callbackUrlSep24;
    String callbackUrlSep31;
    String callbackUrlSep12;
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
