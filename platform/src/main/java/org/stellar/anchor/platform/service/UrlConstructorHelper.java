package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.StringHelper.*;

import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.StringHelper;

public class UrlConstructorHelper {
  /**
   * Add fields from the transaction to the data map. The fields in the extractingFields are
   * extracted from txn and added to the data.
   *
   * @param data the data map
   * @param txn the transaction
   * @param extractingFields the fields to extract from txn
   */
  public static void addTxnFields(
      Map<String, String> data, Sep24Transaction txn, List<String> extractingFields) {
    for (String field : extractingFields) {
      try {
        field = camelToSnake(field);
        String value = BeanUtils.getProperty(txn, snakeToCamelCase(field));
        if (!StringHelper.isEmpty((value))) {
          data.put(field, value);
        }
      } catch (Exception e) {
        // give up. no need to add the field
      }
    }
  }

  public static String getAccount(Sep24Transaction txn) {
    return isEmpty(txn.getSep10AccountMemo())
        ? txn.getSep10Account()
        : txn.getSep10Account() + ":" + txn.getSep10AccountMemo();
  }

  public static ClientsConfig.ClientConfig getClientConfig(
      ClientsConfig clientsConfig, Sep24Transaction txn) throws SepValidationException {
    ClientsConfig.ClientConfig clientConfig;
    if (isEmpty(txn.getClientDomain())) {
      clientConfig = clientsConfig.getClientConfigBySigningKey(txn.getSep10Account());
      if (clientConfig != null && clientConfig.getType() == ClientsConfig.ClientType.NONCUSTODIAL) {
        throw new SepValidationException("Non-custodial clients must specify a client_domain");
      }
    } else {
      clientConfig = clientsConfig.getClientConfigByDomain(txn.getClientDomain());
    }
    return clientConfig;
  }
}
