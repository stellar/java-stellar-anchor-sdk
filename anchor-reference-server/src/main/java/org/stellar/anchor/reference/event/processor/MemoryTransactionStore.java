package org.stellar.anchor.reference.event.processor;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.stellar.anchor.reference.event.ActiveTransactionStore;

/** In-memory implementation of {@link ActiveTransactionStore} for testing purposes. */
@NoArgsConstructor
public class MemoryTransactionStore implements ActiveTransactionStore {
  private final HashMap<String, Set<String>> transactionsByCustomer = new HashMap<>();

  public Set<String> add(String customerId, String transactionId) {
    Set<String> transactionIds = transactionsByCustomer.get(customerId);
    if (transactionIds == null) {
      transactionIds = Set.of(transactionId);
    } else {
      transactionIds.add(transactionId);
    }
    transactionsByCustomer.put(customerId, transactionIds);
    return transactionIds;
  }

  public void remove(String customerId, String transactionId) {
    Set<String> transactionIds = transactionsByCustomer.get(customerId);
    if (transactionIds != null) {
      transactionIds.remove(transactionId);
    }
  }

  @Override
  public Set<String> getTransactions(String customerId) {
    return Optional.ofNullable(transactionsByCustomer.get(customerId)).orElse(Set.of());
  }
}
