package org.stellar.anchor.reference.event;

import java.util.Set;

public interface ActiveTransactionStore {

  Set<String> add(String stellarAccount, String transactionId);

  void remove(String stellarAccount, String transactionId);

  Set<String> getTransactionIdsByStellarAccount(String stellarAccount);
}
