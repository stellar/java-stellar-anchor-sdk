package org.stellar.anchor.platform.rpc;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT_EXCHANGE;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_6;
import static org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_REFUND_PENDING;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.util.AssetHelper.getAssetCode;
import static org.stellar.anchor.util.MathHelper.*;

import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.NotifyRefundPendingRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.RefundPayment;
import org.stellar.anchor.api.shared.Refunds;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.data.*;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;

public class NotifyRefundPendingHandler extends RpcMethodHandler<NotifyRefundPendingRequest> {

  public NotifyRefundPendingHandler(
      Sep6TransactionStore txn6Store,
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    super(
        txn6Store,
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
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
      JdbcSepTransaction txn, NotifyRefundPendingRequest request)
      throws InvalidParamsException, InvalidRequestException {
    String amount;
    String amountFee;
    switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
      case SEP_6:
        JdbcSep6Transaction txn6 = (JdbcSep6Transaction) txn;
        AssetInfo assetInfo6 = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));

        Refunds refunds = txn6.getRefunds();
        amount = request.getRefund().getAmount().getAmount();
        amountFee = request.getRefund().getAmountFee().getAmount();

        BigDecimal totalRefunded6;
        if (refunds == null || refunds.getPayments() == null) {
          totalRefunded6 = sum(assetInfo6, amount, amountFee);
        } else {
          totalRefunded6 =
              sum(assetInfo6, refunds.getAmountRefunded().getAmount(), amount, amountFee);
        }

        BigDecimal amountIn6 = decimal(txn.getAmountIn(), assetInfo6);
        if (totalRefunded6.compareTo(amountIn6) > 0) {
          throw new InvalidParamsException("Refund amount exceeds amount_in");
        }

        return PENDING_EXTERNAL;
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));

        Sep24Refunds sep24Refunds = txn24.getRefunds();
        amount = request.getRefund().getAmount().getAmount();
        amountFee = request.getRefund().getAmountFee().getAmount();

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
    throw new InvalidRequestException(
        String.format(
            "RPC method[%s] is not supported for protocol[%s]", getRpcMethod(), txn.getProtocol()));
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (SEP_6 == Sep.from(txn.getProtocol())) {
      JdbcSep6Transaction txn6 = (JdbcSep6Transaction) txn;
      if (ImmutableSet.of(DEPOSIT, DEPOSIT_EXCHANGE).contains(Kind.from(txn6.getKind()))) {
        return Set.of(PENDING_ANCHOR);
      }
      return emptySet();
    }

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
    NotifyRefundPendingRequest.Refund requestRefund = request.getRefund();
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));

    switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
      case SEP_6:
        JdbcSep6Transaction txn6 = (JdbcSep6Transaction) txn;

        RefundPayment requestPayment =
            RefundPayment.builder()
                .id(requestRefund.getId())
                .idType(RefundPayment.IdType.EXTERNAL)
                .amount(
                    Amount.builder()
                        .amount(requestRefund.getAmount().getAmount())
                        .asset(requestRefund.getAmount().getAsset())
                        .build())
                .fee(
                    Amount.builder()
                        .amount(requestRefund.getAmountFee().getAmount())
                        .asset(requestRefund.getAmountFee().getAsset())
                        .build())
                .build();

        Refunds refunds = txn6.getRefunds();
        if (refunds == null) {
          refunds = new Refunds();
        }

        if (refunds.getPayments() == null) {
          refunds.setPayments(new RefundPayment[] {});
        }
        List<RefundPayment> sep6RefundPayments =
            new ArrayList<>(Arrays.asList(refunds.getPayments()));
        sep6RefundPayments.add(requestPayment);
        refunds.setPayments(sep6RefundPayments.toArray(new RefundPayment[0]));

        // Calculate the total fee amount by summing together fees from all refund payments.
        refunds.setAmountFee(
            new Amount(
                formatAmount(
                    sep6RefundPayments.stream()
                        .map(RefundPayment::getFee)
                        .map(amount -> decimal(amount.getAmount(), assetInfo))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)),
                requestRefund.getAmountFee().getAsset()));

        // Calculate the total refunded amount by summing together amounts from all refund payments.
        refunds.setAmountRefunded(
            new Amount(
                formatAmount(
                    sum(
                        assetInfo,
                        refunds.getAmountFee().getAmount(),
                        formatAmount(
                            sep6RefundPayments.stream()
                                .map(RefundPayment::getAmount)
                                .map(amount -> decimal(amount.getAmount(), assetInfo))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))),
                requestRefund.getAmount().getAsset()));

        txn6.setRefunds(refunds);
        break;
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

        Sep24RefundPayment refundPayment =
            JdbcSep24RefundPayment.builder()
                .id(requestRefund.getId())
                .amount(requestRefund.getAmount().getAmount())
                .fee(requestRefund.getAmountFee().getAmount())
                .build();

        Sep24Refunds sep24Refunds = txn24.getRefunds();
        if (sep24Refunds == null) {
          sep24Refunds = new JdbcSep24Refunds();
        }

        if (sep24Refunds.getRefundPayments() == null) {
          sep24Refunds.setRefundPayments(List.of());
        }
        List<Sep24RefundPayment> sep24RefundPayments = sep24Refunds.getRefundPayments();
        sep24RefundPayments.add(refundPayment);
        sep24Refunds.setRefundPayments(sep24RefundPayments);

        sep24Refunds.recalculateAmounts(assetInfo);
        txn24.setRefunds(sep24Refunds);
        break;
    }
  }
}
