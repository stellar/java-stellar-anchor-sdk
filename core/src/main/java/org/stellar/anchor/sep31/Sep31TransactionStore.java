package org.stellar.anchor.sep31;

import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep31Transaction;

public interface Sep31TransactionStore {
  Sep31Transaction newTransaction();

  Sep31Transaction.Refunds newRefunds();

  Sep31Transaction.RefundPayment newRefundPayment();

  /**
   * Find the Sep31Transaction by transaction_id
   *
   * @param transactionId The transaction ID
   * @return The transaction document. null if not found.
   * @throws SepException if error happens
   */
  Sep31Transaction findByTransactionId(String transactionId) throws SepException;

  /**
   * Save a transaction.
   *
   * @param sep31Transaction The transaction to be saved.
   * @return The saved transaction.
   * @throws SepException SepException.
   */
  @SuppressWarnings("UnusedReturnValue")
  Sep31Transaction save(Sep31Transaction sep31Transaction) throws SepException;
}
