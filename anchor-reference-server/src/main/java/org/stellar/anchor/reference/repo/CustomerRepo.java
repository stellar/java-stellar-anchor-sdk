package org.stellar.anchor.reference.repo;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.stellar.anchor.reference.model.Customer;

public interface CustomerRepo extends CrudRepository<Customer, String> {
  Optional<Customer> findById(@NonNull String Id);

  Optional<Customer> findByStellarAccountAndMemoAndMemoType(@NonNull String stellarAccount, @Nullable String memo, @Nullable String memoType);
}
