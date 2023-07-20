package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_REFUND_INITIATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.util.AssetHelper.getAssetCode;

import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.NotifyRefundInitiatedRequest;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24RefundPayment;
import org.stellar.anchor.platform.data.JdbcSep24Refunds;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyRefundInitiatedHandler extends ActionHandler<NotifyRefundInitiatedRequest> {

  public NotifyRefundInitiatedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService) {
    super(
        txn24Store, txn31Store, requestValidator, assetService, NotifyRefundInitiatedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyRefundInitiatedRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    AssetValidationUtils.validateAsset(
        "refund.amount",
        AmountAssetRequest.builder()
            .amount(request.getRefund().getAmount().getAmount())
            .asset(txn.getAmountInAsset())
            .build(),
        assetService);
    AssetValidationUtils.validateAsset(
        "refund.amountFee",
        AmountAssetRequest.builder()
            .amount(request.getRefund().getAmountFee().getAmount())
            .asset(txn.getAmountInAsset())
            .build(),
        assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_REFUND_INITIATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyRefundInitiatedRequest request) {
    return PENDING_EXTERNAL;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (DEPOSIT == Kind.from(txn24.getKind())) {
        return Set.of(PENDING_ANCHOR);
      }
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyRefundInitiatedRequest request) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    NotifyRefundInitiatedRequest.Refund refund = request.getRefund();
    Sep24RefundPayment refundPayment =
        JdbcSep24RefundPayment.builder()
            .id(refund.getId())
            .amount(refund.getAmount().getAmount())
            .fee(refund.getAmountFee().getAmount())
            .build();

    Sep24Refunds sep24Refunds = txn24.getRefunds();
    if (sep24Refunds == null) {
      sep24Refunds = new JdbcSep24Refunds();
    }

    if (sep24Refunds.getRefundPayments() == null) {
      sep24Refunds.setRefundPayments(List.of());
    }
    List<Sep24RefundPayment> refundPayments = sep24Refunds.getRefundPayments();
    refundPayments.add(refundPayment);
    sep24Refunds.setRefundPayments(refundPayments);

    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));
    sep24Refunds.recalculateAmounts(assetInfo);
    txn24.setRefunds(sep24Refunds);
  }
}
