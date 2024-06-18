package org.stellar.anchor.client;

import java.util.List;

public interface ClientService {
  List<ClientConfig> listAllClients();

  CustodialClientConfig getClientConfigBySigningKey(String signingKey);

  NonCustodialClientConfig getClientConfigByDomain(String domain);

  ClientConfig getClientConfigByDomainAndSep10Account(String domain, String sep10Account);
}
