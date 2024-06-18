package org.stellar.anchor.util;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import org.stellar.anchor.client.ClientConfig;
import org.stellar.anchor.client.ClientService;
import org.stellar.anchor.sep24.Sep24Transaction;

public final class ConfigHelper {
  /**
   * Gets client config from the config collection that matches the predicate. If clientDomain is
   * provided, lookup in the collection by the domain. If it's not provided, lookup by the SEP-10
   * account.
   *
   * @param clientService client service to use for the lookup
   * @param clientDomain domain to use for the lookup
   * @param sep10Account SEP-10 authenticated account to use for lookup
   * @return client config by the predicate. If not found, returns null.
   */
  public static ClientConfig getClientConfig(
      ClientService clientService, String clientDomain, String sep10Account) {
    ClientConfig clientConfig;
    if (isEmpty(clientDomain)) {
      clientConfig = clientService.getClientConfigBySigningKey(sep10Account);
    } else {
      clientConfig = clientService.getClientConfigByDomain(clientDomain);
    }
    return clientConfig;
  }

  public static ClientConfig getClientConfig(ClientService clientService, Sep24Transaction txn) {
    return getClientConfig(clientService, txn.getClientDomain(), txn.getSep10Account());
  }
}
