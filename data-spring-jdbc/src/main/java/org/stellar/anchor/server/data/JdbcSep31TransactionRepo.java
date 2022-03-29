package org.stellar.anchor.server.data;

import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;

public interface JdbcSep31TransactionRepo extends CrudRepository<JdbcSep31Transaction, String> {
  Optional<JdbcSep31Transaction> findById(@NonNull String id);
}
