package org.stellar.anchor.platform.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.stellar.anchor.sep31.Sep31Transaction;

public interface JdbcSep31TransactionRepo
    extends PagingAndSortingRepository<JdbcSep31Transaction, String>,
        AllTransactionsRepository<JdbcSep31Transaction> {
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
}
