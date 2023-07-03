package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.ActionMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.rpc.ActionMethod;
import org.stellar.anchor.api.rpc.NotifyInteractiveFlowCompletedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyInteractiveFlowCompletedHandler
    extends ActionHandler<NotifyInteractiveFlowCompletedRequest> {

  public NotifyInteractiveFlowCompletedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
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
      throws BadRequestException {
    validateAsset("amount_in", request.getAmountIn());
    validateAsset("amount_out", request.getAmountOut());
    validateAsset("amount_fee", request.getAmountFee(), true);
    validateAsset("amount_expected", request.getAmountExpected());

    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    txn24.setAmountIn(request.getAmountIn().getAmount());
    txn24.setAmountInAsset(request.getAmountIn().getAsset());

    txn24.setAmountIn(request.getAmountOut().getAmount());
    txn24.setAmountInAsset(request.getAmountOut().getAsset());

    txn24.setAmountIn(request.getAmountFee().getAmount());
    txn24.setAmountInAsset(request.getAmountFee().getAsset());

    if (request.getAmountExpected() != null) {
      txn24.setAmountExpected(request.getAmountFee().getAmount());
    } else {
      txn24.setAmountExpected(txn.getAmountIn());
    }
  }
}
