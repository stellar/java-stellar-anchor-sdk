package org.stellar.anchor.server.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.stellar.anchor.model.Sep24Transaction;

public interface JdbcSep24TransactionRepo extends CrudRepository<JdbcSep24Transaction, String> {
  Optional<JdbcSep24Transaction> findById(@NonNull String id);

  JdbcSep24Transaction findOneByTransactionId(String transactionId);

  JdbcSep24Transaction findOneByExternalTransactionId(String externalTransactionId);

  JdbcSep24Transaction findOneByStellarTransactionId(String stellarTransactionId);

  List<Sep24Transaction> findByStellarAccountAndAssetCodeOrderByStartedAtDesc(
      String stellarAccount, String assetCode);
}
