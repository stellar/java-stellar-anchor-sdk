package org.stellar.anchor.sep24;

import java.util.List;
import org.stellar.anchor.dto.sep24.GetTransactionsRequest;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep24Transaction;

/** This interface is for the SEP adapter service to query/save the transaction document. */
public interface Sep24TransactionStore {
  Sep24Transaction newInstance();

  /**
   * Find the Sep24Transaction by transaction_id
   *
   * @param transactionId The transaction ID
   * @return The transaction document. null if not found.
   */
  Sep24Transaction findByTransactionId(String transactionId) throws SepException;

  /**
   * Find the Sep24Transaction by the stellar network transaction id (hash)
   *
   * @param stellarTransactionId The Stellar transaction id (hash)
   * @return The transaction document. null if not found.
   */
  Sep24Transaction findByStellarTransactionId(String stellarTransactionId) throws SepException;

  /**
   * Find the Sep24Transaction by the anchor's transaction Id.
   *
   * @param externalTransactionId The anchor's transaction id.
   * @return The transaction document. null if not found.
   */
  Sep24Transaction findByExternalTransactionId(String externalTransactionId) throws SepException;

  /**
   * Find the transactions filtered and limited the request
   *
   * @param accountId The Stellar account id.
   * @param request The query request.
   * @return The list of transaction documents. If not found, return empty list.
   */
  List<Sep24Transaction> findTransactions(
      String accountId, String accountMemo, GetTransactionsRequest request) throws SepException;

  /**
   * Save a transaction.
   *
   * @param sep24Transaction The transaction to be saved.
   * @return The saved transaction.
   * @throws SepException SepException.
   */
  @SuppressWarnings("UnusedReturnValue")
  Sep24Transaction save(Sep24Transaction sep24Transaction) throws SepException;
}
