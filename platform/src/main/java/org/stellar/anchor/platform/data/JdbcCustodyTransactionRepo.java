package org.stellar.anchor.platform.data;

import org.springframework.data.repository.CrudRepository;

public interface JdbcCustodyTransactionRepo extends CrudRepository<JdbcCustodyTransaction, String> {

  JdbcCustodyTransaction findByExternalTxId(String externalTxId);

  JdbcCustodyTransaction findByToAccountAndMemo(String toAccount, String memo);
}
