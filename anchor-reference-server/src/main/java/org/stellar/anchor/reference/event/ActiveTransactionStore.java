package org.stellar.anchor.reference.event;

import java.util.Set;

/** Stores the transactions that are currently being processed by the anchor. */
public interface ActiveTransactionStore {

  /**
   * Adds a transaction to the store.
   *
   * @param stellarAccount The stellar account associated with the transaction.
   * @param transactionId The transaction id.
   * @return The set of transactions that are currently being processed for the account.
   */
  Set<String> add(String stellarAccount, String transactionId);

  /**
   * Removes a transaction from the store.
   *
   * @param stellarAccount The stellar account associated with the transaction.
   * @param transactionId The transaction id.
   */
  void remove(String stellarAccount, String transactionId);

  /**
   * Gets the transactions that are currently being processed for the account.
   *
   * @param stellarAccount The stellar account associated with the transaction.
   * @return The set of transactions that are currently being processed for the account.
   */
  Set<String> getTransactions(String stellarAccount);
}
