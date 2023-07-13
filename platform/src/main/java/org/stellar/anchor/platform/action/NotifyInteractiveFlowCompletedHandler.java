package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.util.HashSet;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.NotifyInteractiveFlowCompletedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyInteractiveFlowCompletedHandler
    extends ActionHandler<NotifyInteractiveFlowCompletedRequest> {

  public NotifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        horizon,
        assetService,
        NotifyInteractiveFlowCompletedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyInteractiveFlowCompletedRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    AssetValidationUtils.validateAsset("amount_in", request.getAmountIn(), assetService);
    AssetValidationUtils.validateAsset("amount_out", request.getAmountOut(), assetService);
    AssetValidationUtils.validateAsset("amount_fee", request.getAmountFee(), true, assetService);
    if (request.getAmountExpected() != null) {
      AssetValidationUtils.validateAsset(
          "amount_expected",
          AmountAssetRequest.builder()
              .amount(request.getAmountExpected().getAmount())
              .asset(request.getAmountIn().getAsset())
              .build(),
          assetService);
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
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      supportedStatuses.add(INCOMPLETE);
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyInteractiveFlowCompletedRequest request) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    txn24.setAmountIn(request.getAmountIn().getAmount());
    txn24.setAmountInAsset(request.getAmountIn().getAsset());

    txn24.setAmountOut(request.getAmountOut().getAmount());
    txn24.setAmountOutAsset(request.getAmountOut().getAsset());

    txn24.setAmountFee(request.getAmountFee().getAmount());
    txn24.setAmountFeeAsset(request.getAmountFee().getAsset());

    if (request.getAmountExpected() != null) {
      txn24.setAmountExpected(request.getAmountExpected().getAmount());
    } else {
      txn24.setAmountExpected(txn.getAmountIn());
    }
  }
}
