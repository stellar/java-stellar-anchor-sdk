package org.stellar.anchor.platform.data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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

  @Query(
      value =
          "SELECT t.* from sep24_transaction t where t.started_at >= :from and t.started_at <= :to order by started_at asc limit :lim offset :offset",
      nativeQuery = true)
  List<JdbcSep24Transaction> findStarted(
      @Param("to") Instant to,
      @Param("from") Instant from,
      @Param("lim") Integer limit,
      @Param("offset") Integer offset);

  @Query(
      value =
          "SELECT t.* from sep24_transaction t where t.transfer_received_at >= :from and t.transfer_received_at <= :to order by transfer_received_at asc limit :lim offset :offset",
      nativeQuery = true)
  List<JdbcSep24Transaction> findTransferred(
      @Param("to") Instant to,
      @Param("from") Instant from,
      @Param("lim") Integer limit,
      @Param("offset") Integer offset);
}
