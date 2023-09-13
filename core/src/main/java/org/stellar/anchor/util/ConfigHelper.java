package org.stellar.anchor.util;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.sep24.Sep24Transaction;

public final class ConfigHelper {
  /**
   * Gets client config from the config collection that matches the predicate. If clientDomain is
   * provided, lookup in the collection by the domain. If it's not provided, lookup by the SEP-10
   * account.
   *
   * @param clientsConfig collection of client configurations
   * @param clientDomain domain to use for the lookup
   * @param sep10Account SEP-10 authenticated account to use for lookup
   * @return client config by the predicate. If not found, returns null.
   */
  public static ClientsConfig.ClientConfig getClientConfig(
      ClientsConfig clientsConfig, String clientDomain, String sep10Account)
      throws SepValidationException {
    ClientsConfig.ClientConfig clientConfig;
    if (isEmpty(clientDomain)) {
      clientConfig = clientsConfig.getClientConfigBySigningKey(sep10Account);
      if (clientConfig != null && clientConfig.getType() == ClientsConfig.ClientType.NONCUSTODIAL) {
        throw new SepValidationException("Non-custodial clients must specify a client_domain");
      }
    } else {
      clientConfig = clientsConfig.getClientConfigByDomain(clientDomain);
    }
    return clientConfig;
  }

  public static ClientsConfig.ClientConfig getClientConfig(
      ClientsConfig clientsConfig, Sep24Transaction txn) throws SepValidationException {
    return getClientConfig(clientsConfig, txn.getClientDomain(), txn.getSep10Account());
  }
}
