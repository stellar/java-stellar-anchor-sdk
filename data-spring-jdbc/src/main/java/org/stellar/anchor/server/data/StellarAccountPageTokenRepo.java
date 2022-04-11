package org.stellar.anchor.server.data;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface StellarAccountPageTokenRepo
    extends CrudRepository<StellarAccountPageToken, String> {
  Optional<StellarAccountPageToken> findById(String id);

  Optional<StellarAccountPageToken> findByAccountId(String accountId);
}
