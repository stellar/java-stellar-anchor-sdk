package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_AMOUNTS_UPDATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountRequest;
import org.stellar.anchor.api.rpc.action.NotifyAmountsUpdatedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyAmountsUpdatedHandler extends ActionHandler<NotifyAmountsUpdatedRequest> {

  public NotifyAmountsUpdatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyAmountsUpdatedRequest request)
      throws InvalidParamsException, InvalidRequestException {
    super.validate(txn, request);

    validateAsset(
        "amount_out",
        AmountRequest.builder()
            .amount(request.getAmountOut())
            .asset(txn.getAmountOutAsset())
            .build());
    validateAsset(
        "amount_fee",
        AmountRequest.builder()
            .amount(request.getAmountFee())
            .asset(txn.getAmountFeeAsset())
            .build(),
        true);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_AMOUNTS_UPDATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyAmountsUpdatedRequest request) {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (txn24.getTransferReceivedAt() != null) {
      return Set.of(PENDING_ANCHOR);
    }
    return Set.of();
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyAmountsUpdatedRequest request) throws InvalidParamsException {
    txn.setAmountOut(request.getAmountOut());
    txn.setAmountFee(request.getAmountFee());
  }
}
