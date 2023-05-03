package org.stellar.anchor.platform.data;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.stellar.anchor.sep31.Sep31Transaction;

public interface JdbcSep31TransactionRepo extends CrudRepository<JdbcSep31Transaction, String> {
  Optional<JdbcSep31Transaction> findById(@NonNull String id);

  @Query(value = "SELECT t FROM JdbcSep31Transaction t WHERE t.id IN :ids")
  List<JdbcSep31Transaction> findByIds(@Param("ids") Collection<String> ids);

  Optional<JdbcSep31Transaction> findByStellarAccountId(@NonNull String stellarAccountId);

  Optional<Sep31Transaction> findByStellarMemo(String stellarMemo);

  @Query(value = "SELECT COUNT(t) FROM JdbcSep31Transaction t WHERE t.status = :status")
  Integer findByStatusCount(@Param("status") String status);

  Optional<JdbcSep31Transaction> findByStellarAccountIdAndStellarMemo(
      @Param("stellar_account_id") String stellarAccountId,
      @Param("stellar_memo") String stellarMemo);

  @Query(
      value =
          "SELECT t.* from sep31_transaction t where t.started_at >= :from and t.started_at <= :to order by started_at asc limit :lim offset :offset",
      nativeQuery = true)
  List<JdbcSep31Transaction> findStarted(
      @Param("to") Instant to,
      @Param("from") Instant from,
      @Param("lim") Integer limit,
      @Param("offset") Integer offset);

  @Query(
      value =
          "SELECT t.* from sep31_transaction t where t.transfer_received_at >= :from and t.transfer_received_at <= :to order by transfer_received_at asc limit :lim offset :offset",
      nativeQuery = true)
  List<JdbcSep31Transaction> findTransferred(
      @Param("to") Instant to,
      @Param("from") Instant from,
      @Param("lim") Integer limit,
      @Param("offset") Integer offset);
}
