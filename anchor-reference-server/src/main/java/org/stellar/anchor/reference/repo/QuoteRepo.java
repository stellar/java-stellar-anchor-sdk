package org.stellar.anchor.reference.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.stellar.anchor.reference.model.Quote;

public interface QuoteRepo extends CrudRepository<Quote, String> {
  Optional<Quote> findById(@NonNull String Id);
}
