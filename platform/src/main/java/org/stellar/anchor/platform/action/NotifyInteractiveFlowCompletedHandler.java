package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.util.Set;
import javax.validation.Validator;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountRequest;
import org.stellar.anchor.api.rpc.action.NotifyInteractiveFlowCompletedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyInteractiveFlowCompletedHandler
    extends ActionHandler<NotifyInteractiveFlowCompletedRequest> {

  public NotifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    super(
        txn24Store,
        txn31Store,
        validator,
        horizon,
        assetService,
        NotifyInteractiveFlowCompletedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyInteractiveFlowCompletedRequest request)
      throws InvalidParamsException, InvalidRequestException {
    super.validate(txn, request);

    validateAsset("amount_in", request.getAmountIn());
    validateAsset("amount_out", request.getAmountOut());
    validateAsset("amount_fee", request.getAmountFee(), true);
    if (request.getAmountExpected() != null) {
      validateAsset(
          "amount_expected",
          AmountRequest.builder()
              .amount(request.getAmountExpected())
              .asset(request.getAmountIn().getAsset())
              .build());
    }
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_INTERACTIVE_FLOW_COMPLETED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyInteractiveFlowCompletedRequest request) {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    return Set.of(INCOMPLETE);
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyInteractiveFlowCompletedRequest request)
      throws InvalidParamsException {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    txn24.setAmountIn(request.getAmountIn().getAmount());
    txn24.setAmountInAsset(request.getAmountIn().getAsset());

    txn24.setAmountOut(request.getAmountOut().getAmount());
    txn24.setAmountOutAsset(request.getAmountOut().getAsset());

    txn24.setAmountFee(request.getAmountFee().getAmount());
    txn24.setAmountFeeAsset(request.getAmountFee().getAsset());

    if (request.getAmountExpected() != null) {
      txn24.setAmountExpected(request.getAmountExpected());
    } else {
      txn24.setAmountExpected(txn.getAmountIn());
    }
  }
}
