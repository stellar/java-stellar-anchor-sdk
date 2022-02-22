package org.stellar.anchor.reference.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.stellar.anchor.reference.model.Customer;

import java.util.Optional;

public interface CustomerRepo extends CrudRepository<Customer, String> {
  Optional<Customer> findById(@NonNull String Id);

  Optional<Customer> findByStellarAccount(@NonNull String stellarAccount);
}
