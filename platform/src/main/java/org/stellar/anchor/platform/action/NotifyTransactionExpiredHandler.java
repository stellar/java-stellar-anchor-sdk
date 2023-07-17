package org.stellar.anchor.platform.action;

import static java.util.stream.Collectors.toSet;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;

import java.util.Arrays;
import java.util.Set;
import javax.validation.Validator;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTransactionExpiredRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTransactionExpiredHandler
    extends ActionHandler<NotifyTransactionExpiredRequest> {

  private final JdbcTransactionPendingTrustRepo transactionPendingTrustRepo;

  public NotifyTransactionExpiredHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    super(
        txn24Store,
        txn31Store,
        validator,
        horizon,
        assetService,
        NotifyTransactionExpiredRequest.class);
    this.transactionPendingTrustRepo = transactionPendingTrustRepo;
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRANSACTION_EXPIRED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {
    return EXPIRED;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    return Arrays.stream(SepTransactionStatus.values())
        .filter(s -> !isErrorStatus(s) && !isFinalStatus(s))
        .collect(toSet());
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {
    if (transactionPendingTrustRepo.existsById(txn.getId())) {
      transactionPendingTrustRepo.deleteById(txn.getId());
    }
  }
}
