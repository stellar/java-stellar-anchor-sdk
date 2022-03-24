package org.stellar.anchor.server.data;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

public interface JdbcSep38QuoteRepo extends CrudRepository<JdbcSep38Quote, String> {
  Optional<JdbcSep38Quote> findById(@NonNull String id);
}
