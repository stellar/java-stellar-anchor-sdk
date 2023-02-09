package org.stellar.anchor.platform.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.stellar.anchor.sep24.Sep24Transaction;

public interface JdbcSep24TransactionRepo extends CrudRepository<JdbcSep24Transaction, String> {
  Optional<JdbcSep24Transaction> findById(@NonNull String id);

  JdbcSep24Transaction findOneByTransactionId(String transactionId);

  JdbcSep24Transaction findOneByExternalTransactionId(String externalTransactionId);

  JdbcSep24Transaction findOneByStellarTransactionId(String stellarTransactionId);

  JdbcSep24Transaction findOneBySep10AccountAndMemo(String accountId, String memo);

  List<Sep24Transaction> findBySep10AccountAndRequestAssetCodeOrderByStartedAtDesc(
      String stellarAccount, String assetCode);
}
