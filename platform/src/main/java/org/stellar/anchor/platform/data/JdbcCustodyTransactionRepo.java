package org.stellar.anchor.platform.data;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface JdbcCustodyTransactionRepo extends CrudRepository<JdbcCustodyTransaction, String> {

  JdbcCustodyTransaction findByExternalTxId(String externalTxId);

  JdbcCustodyTransaction findByToAccountAndMemo(String toAccount, String memo);

  List<JdbcCustodyTransaction> findAllByStatusAndExternalTxIdNotNull(String status);
}
