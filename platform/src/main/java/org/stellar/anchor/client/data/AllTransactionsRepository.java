package org.stellar.anchor.client.data;

import java.util.List;
import org.stellar.anchor.util.TransactionsParams;

public interface AllTransactionsRepository<T> {
  List<T> findAllTransactions(TransactionsParams params, Class<T> entityClass);
}
