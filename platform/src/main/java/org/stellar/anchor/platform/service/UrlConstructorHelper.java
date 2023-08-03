package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.StringHelper.*;

import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.stellar.anchor.sep24.Sep24Transaction;

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
        if (!isEmpty((value))) {
          data.put(field, value);
        }
      } catch (Exception e) {
        // give up. no need to add the field
      }
    }
  }

  /**
   * Get the account from the transaction. If the memo is not empty, this returns the SEP-10 account
   * concatenated with the memo. Otherwise, it returns the SEP-10 account.
   *
   * @param txn
   * @return the account
   */
  public static String getAccount(Sep24Transaction txn) {
    return isEmpty(txn.getSep10AccountMemo())
        ? txn.getSep10Account()
        : txn.getSep10Account() + ":" + txn.getSep10AccountMemo();
  }
}
