package org.stellar.anchor.reference.event.processor;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.stellar.anchor.reference.event.ActiveTransactionStore;

/** In-memory implementation of {@link ActiveTransactionStore} for testing purposes. */
@NoArgsConstructor
public class MemoryTransactionStore implements ActiveTransactionStore {
  private final HashMap<String, Set<String>> transactionsByStellarAccount = new HashMap<>();

  public Set<String> add(String stellarAccount, String transactionId) {
    Set<String> transactionIds = transactionsByStellarAccount.get(stellarAccount);
    if (transactionIds == null) {
      transactionIds = Set.of(transactionId);
    } else {
      transactionIds.add(transactionId);
    }
    transactionsByStellarAccount.put(stellarAccount, transactionIds);
    return transactionIds;
  }

  public void remove(String stellarAccount, String transactionId) {
    Set<String> transactionIds = transactionsByStellarAccount.get(stellarAccount);
    if (transactionIds != null) {
      transactionIds.remove(transactionId);
    }
  }

  @Override
  public Set<String> getTransactions(String stellarAccount) {
    return Optional.of(transactionsByStellarAccount.get(stellarAccount)).orElse(Set.of());
  }
}
