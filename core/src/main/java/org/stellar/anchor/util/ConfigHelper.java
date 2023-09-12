package org.stellar.anchor.util;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.sep24.Sep24Transaction;

public final class ConfigHelper {
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
      clientConfig = clientsConfig.getClientConfigByDomain(sep10Account);
    }
    return clientConfig;
  }

  public static ClientsConfig.ClientConfig getClientConfig(
      ClientsConfig clientsConfig, Sep24Transaction txn) throws SepValidationException {
    return getClientConfig(clientsConfig, txn.getClientDomain(), txn.getSep10Account());
  }
}
