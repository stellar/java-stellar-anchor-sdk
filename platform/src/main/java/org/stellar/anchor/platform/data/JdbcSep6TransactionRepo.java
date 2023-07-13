package org.stellar.anchor.platform.data;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.stellar.anchor.sep6.Sep6Transaction;

public interface JdbcSep6TransactionRepo
    extends PagingAndSortingRepository<JdbcSep6Transaction, String>,
        AllTransactionsRepository<JdbcSep6Transaction> {
  Sep6Transaction findOneByTransactionId(String transactionId);

  Sep6Transaction findOneByStellarTransactionId(String stellarTransactionId);

  Sep6Transaction findOneByExternalTransactionId(String externalTransactionId);
}
