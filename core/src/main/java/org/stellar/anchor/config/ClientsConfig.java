package org.stellar.anchor.config;

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
    String signingKey;
    String domain;
    String callbackUrl;
  }

  enum ClientType {
    CUSTODIAL,
    NONCUSTODIAL
  }

  ClientConfig getClientConfigBySigningKey(String signingKey);

  ClientConfig getClientConfigByDomain(String domain);
}
