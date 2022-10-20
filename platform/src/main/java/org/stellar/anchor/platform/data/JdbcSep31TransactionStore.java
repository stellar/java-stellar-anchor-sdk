package org.stellar.anchor.platform.data;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.sep31.RefundPayment;
import org.stellar.anchor.sep31.Refunds;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class JdbcSep31TransactionStore implements Sep31TransactionStore {
  private final JdbcSep31TransactionRepo transactionRepo;

  public JdbcSep31TransactionStore(JdbcSep31TransactionRepo transactionRepo) {
    this.transactionRepo = transactionRepo;
  }

  @Override
  public Sep31Transaction newTransaction() {
    return new JdbcSep31Transaction();
  }

  @Override
  public Refunds newRefunds() {
    return new JdbcSep31Refunds();
  }

  @Override
  public RefundPayment newRefundPayment() {
    return new JdbcSep31RefundPayment();
  }

  @Override
  public Sep31Transaction findByTransactionId(@NonNull String transactionId)
      throws AnchorException {
    return transactionRepo.findById(transactionId).orElse(null);
  }

  @Override
  public List<? extends Sep31Transaction> findByTransactionIds(
      @NonNull Collection<String> transactionId) throws AnchorException {
    return transactionRepo.findByIds(transactionId);
  }

  @Override
  public Sep31Transaction findByStellarMemo(@NonNull String memo) throws AnchorException {
    return transactionRepo.findByStellarMemo(memo).orElse(null);
  }

  @Override
  public Sep31Transaction save(Sep31Transaction transaction) throws SepException {
    if (!(transaction instanceof JdbcSep31Transaction)) {
      throw new SepException(
          transaction.getClass() + "  is not a sub-type of " + JdbcSep31Transaction.class);
    }
    JdbcSep31Transaction txn = (JdbcSep31Transaction) transaction;

    txn.setUpdatedAt(Instant.now());
    if (txn.getAmountExpected() == null) {
      txn.setAmountExpected(txn.getAmountIn());
    }

    return transactionRepo.save((JdbcSep31Transaction) transaction);
  }

  public Sep31Transaction findByStellarAccountId(String accountId) {
    Optional<JdbcSep31Transaction> optTxn = transactionRepo.findByStellarAccountId(accountId);
    return optTxn.orElse(null);
  }

  public Integer findByStatusCount(String status) {
    return transactionRepo.findByStatusCount(status);
  }
}
