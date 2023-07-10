package org.stellar.anchor.platform.data;

import java.util.List;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.sep6.*;
import org.stellar.anchor.util.TransactionsParams;

public class JdbcSep6TransactionStore implements Sep6TransactionStore {
  private final JdbcSep6TransactionRepo transactionRepo;

  public JdbcSep6TransactionStore(JdbcSep6TransactionRepo transactionRepo) {
    this.transactionRepo = transactionRepo;
  }

  @Override
  public Sep6Transaction newInstance() {
    return new JdbcSep6Transaction();
  }

  @Override
  public Sep6Refunds newRefunds() {
    return new JdbcSep6Refunds();
  }

  @Override
  public Sep6RefundPayment newRefundPayment() {
    return new JdbcSep6RefundPayment();
  }

  @Override
  public Sep6Transaction findByTransactionId(String transactionId) {
    return transactionRepo.findOneByTransactionId(transactionId);
  }

  @Override
  public Sep6Transaction findByStellarTransactionId(String stellarTransactionId) {
    return transactionRepo.findOneByStellarTransactionId(stellarTransactionId);
  }

  @Override
  public Sep6Transaction findByExternalTransactionId(String externalTransactionId) {
    return transactionRepo.findOneByExternalTransactionId(externalTransactionId);
  }

  @Override
  public List<Sep6Transaction> findTransactions(
      String accountId, String accountMemo, GetTransactionsRequest request) {
    // TODO: implement with GET /transactions endpoint
    return null;
  }

  @Override
  public Sep6Transaction save(Sep6Transaction sep6Transaction) throws SepException {
    if (!(sep6Transaction instanceof JdbcSep6Transaction)) {
      throw new SepException(
          sep6Transaction.getClass() + " is not a sub-type of " + JdbcSep6Transaction.class);
    }
    JdbcSep6Transaction txn = (JdbcSep6Transaction) sep6Transaction;
    txn.setId(txn.getTransactionId());
    return transactionRepo.save(txn);
  }

  @Override
  public List<? extends Sep6Transaction> findTransactions(TransactionsParams params) {
    return transactionRepo.findAllTransactions(params, JdbcSep6Transaction.class);
  }
}
