package org.stellar.anchor.server.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface JdbcSep31TransactionRepo extends CrudRepository<JdbcSep31Transaction, String> {
  Optional<JdbcSep31Transaction> findById(@NonNull String id);

  @Query(value = "SELECT t FROM JdbcSep31Transaction t WHERE t.id IN :ids")
  List<JdbcSep31Transaction> findByIds(@Param("ids") Collection<String> ids);

  Optional<JdbcSep31Transaction> findByStellarAccountId(@NonNull String stellarAccountId);
}
