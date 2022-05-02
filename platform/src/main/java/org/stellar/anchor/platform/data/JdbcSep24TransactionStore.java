package org.stellar.anchor.platform.data;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.sep24.GetTransactionsRequest;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.util.DateUtil;

public class JdbcSep24TransactionStore implements Sep24TransactionStore {
  static final String CB_KEY_NAMESPACE = "SAS:RESOURCE:";
  final JdbcSep24TransactionRepo txnRepo;

  public JdbcSep24TransactionStore(JdbcSep24TransactionRepo txnRepo) {
    this.txnRepo = txnRepo;
  }

  @Override
  public Sep24Transaction newInstance() {
    return new JdbcSep24Transaction();
  }

  @Override
  public Sep24Transaction findByTransactionId(String transactionId) {
    return txnRepo.findOneByTransactionId(transactionId);
  }

  @Override
  public Sep24Transaction findByStellarTransactionId(String stellarTransactionId) {
    return txnRepo.findOneByStellarTransactionId(stellarTransactionId);
  }

  @Override
  public Sep24Transaction findByExternalTransactionId(String externalTransactionId) {
    return txnRepo.findOneByExternalTransactionId(externalTransactionId);
  }

  @Override
  public List<Sep24Transaction> findTransactions(String accountId, GetTransactionsRequest tr)
      throws SepValidationException {
    List<Sep24Transaction> txns =
        txnRepo.findByStellarAccountAndAssetCodeOrderByStartedAtDesc(accountId, tr.getAssetCode());

    // TODO: This should be replaced by Couchbase query
    int limit = Integer.MAX_VALUE;
    if (tr.getLimit() != null && tr.getLimit() > 0) {
      limit = tr.getLimit();
    }

    final long noOlderThan;
    final long olderThan;

    if (tr.getPagingId() != null) {
      Sep24Transaction txn = txnRepo.findOneByTransactionId(tr.getPagingId());
      if (txn == null) {
        olderThan = System.currentTimeMillis() / 1000;
      } else {
        olderThan = txn.getStartedAt();
      }
    } else {
      olderThan = System.currentTimeMillis() / 1000;
    }

    if (tr.getNoOlderThan() == null) {
      noOlderThan = 0L;
    } else {
      try {
        noOlderThan = DateUtil.fromISO8601UTC(tr.getNoOlderThan());
      } catch (DateTimeParseException dtpex) {
        throw new SepValidationException(
            String.format("invalid no_older_than field: %s", tr.getNoOlderThan()));
      }
    }

    txns =
        txns.stream()
            .filter(txn -> (tr.getKind() == null || tr.getKind().equals(txn.getKind())))
            .filter(txn -> (txn.getStartedAt() >= noOlderThan - 1))
            .filter(txn -> (txn.getStartedAt() < olderThan - 1))
            .limit(limit)
            .collect(Collectors.toList());
    return txns;
  }

  @Override
  public Sep24Transaction save(Sep24Transaction sep24Transaction) throws SepException {
    if (!(sep24Transaction instanceof JdbcSep24Transaction)) {
      throw new SepException(
          sep24Transaction.getClass() + "  is not a sub-type of " + JdbcSep24Transaction.class);
    }
    JdbcSep24Transaction txn = (JdbcSep24Transaction) sep24Transaction;
    txn.setId(txn.getTransactionId());
    return txnRepo.save(txn);
  }
}
