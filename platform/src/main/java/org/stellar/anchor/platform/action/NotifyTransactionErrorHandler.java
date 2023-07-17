package org.stellar.anchor.platform.action;

import static java.util.stream.Collectors.toSet;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;

import java.util.Arrays;
import java.util.Set;
import javax.validation.Validator;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTransactionErrorRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTransactionErrorHandler extends ActionHandler<NotifyTransactionErrorRequest> {

  private final JdbcTransactionPendingTrustRepo transactionPendingTrustRepo;

  public NotifyTransactionErrorHandler(
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
        NotifyTransactionErrorRequest.class);
    this.transactionPendingTrustRepo = transactionPendingTrustRepo;
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRANSACTION_ERROR;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionErrorRequest request) {
    return ERROR;
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
      JdbcSepTransaction txn, NotifyTransactionErrorRequest request) {
    if (transactionPendingTrustRepo.existsById(txn.getId())) {
      transactionPendingTrustRepo.deleteById(txn.getId());
    }
  }
}
