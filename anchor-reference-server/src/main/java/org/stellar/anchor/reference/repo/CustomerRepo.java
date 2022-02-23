package org.stellar.anchor.reference.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.stellar.anchor.reference.model.Customer;

public interface CustomerRepo extends CrudRepository<Customer, String> {
  Optional<Customer> findById(@NonNull String Id);

  Optional<Customer> findByStellarAccount(@NonNull String stellarAccount);
}
