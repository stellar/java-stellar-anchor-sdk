package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_AMOUNTS_UPDATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTransactionRecoveryRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyTransactionRecoveryHandler
    extends ActionHandler<NotifyTransactionRecoveryRequest> {

  public NotifyTransactionRecoveryHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_AMOUNTS_UPDATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionRecoveryRequest request) {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (txn24.getTransferReceivedAt() != null) {
      return Set.of(ERROR, EXPIRED);
    }
    return Set.of();
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTransactionRecoveryRequest request)
      throws InvalidParamsException {}
}
