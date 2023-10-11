package org.stellar.anchor.platform.rpc;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_REFUND_PENDING;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.util.AssetHelper.getAssetCode;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.sum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.NotifyRefundPendingRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
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

public class NotifyRefundPendingHandler extends RpcMethodHandler<NotifyRefundPendingRequest> {

  public NotifyRefundPendingHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        NotifyRefundPendingRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyRefundPendingRequest request)
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
        true,
        assetService);

    if (!txn.getAmountInAsset().equals(request.getRefund().getAmount().getAsset())) {
      throw new InvalidParamsException(
          "refund.amount.asset does not match transaction amount_in_asset");
    }
    if (!txn.getAmountFeeAsset().equals(request.getRefund().getAmountFee().getAsset())) {
      throw new InvalidParamsException(
          "refund.amount_fee.asset does not match match transaction amount_fee_asset");
    }
  }

  @Override
  public RpcMethod getRpcMethod() {
    return NOTIFY_REFUND_PENDING;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyRefundPendingRequest request) throws InvalidParamsException {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));

    Sep24Refunds sep24Refunds = txn24.getRefunds();
    String amount = request.getRefund().getAmount().getAmount();
    String amountFee = request.getRefund().getAmountFee().getAmount();

    BigDecimal totalRefunded;
    if (sep24Refunds == null || sep24Refunds.getRefundPayments() == null) {
      totalRefunded = sum(assetInfo, amount, amountFee);
    } else {
      totalRefunded = sum(assetInfo, sep24Refunds.getAmountRefunded(), amount, amountFee);
    }

    BigDecimal amountIn = decimal(txn.getAmountIn(), assetInfo);
    if (totalRefunded.compareTo(amountIn) > 0) {
      throw new InvalidParamsException("Refund amount exceeds amount_in");
    }

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
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, NotifyRefundPendingRequest request) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    NotifyRefundPendingRequest.Refund refund = request.getRefund();
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
