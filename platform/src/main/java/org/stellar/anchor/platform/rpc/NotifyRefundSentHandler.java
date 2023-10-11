package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE;
import static org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_REFUND_SENT;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_STELLAR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.REFUNDED;
import static org.stellar.anchor.util.AssetHelper.getAssetCode;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.sum;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.NotifyRefundSentRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24RefundPayment;
import org.stellar.anchor.platform.data.JdbcSep24Refunds;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31RefundPayment;
import org.stellar.anchor.platform.data.JdbcSep31Refunds;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.RefundPayment;
import org.stellar.anchor.sep31.Sep31Refunds;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyRefundSentHandler extends RpcMethodHandler<NotifyRefundSentRequest> {

  public NotifyRefundSentHandler(
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
        NotifyRefundSentRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyRefundSentRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    SepTransactionStatus currentStatus = SepTransactionStatus.from(txn.getStatus());
    switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
      case SEP_24:
        if (request.getRefund() == null && PENDING_ANCHOR == currentStatus) {
          throw new InvalidParamsException("refund is required");
        }
        break;
      case SEP_31:
        JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
        Sep31Refunds sep31Refunds = txn31.getRefunds();
        if (request.getRefund() == null) {
          throw new InvalidParamsException("refund is required");
        }

        if (PENDING_RECEIVER == currentStatus) {
          if (sep31Refunds != null
              && sep31Refunds.getRefundPayments() != null
              && !sep31Refunds.getRefundPayments().isEmpty()) {
            throw new InvalidRequestException(
                String.format(
                    "Multiple refunds aren't supported for kind[%s], protocol[%s] and action[%s]",
                    RECEIVE, txn.getProtocol(), getRpcMethod()));
          }
        }
    }

    if (request.getRefund() != null) {
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
  }

  @Override
  public RpcMethod getRpcMethod() {
    return NOTIFY_REFUND_SENT;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyRefundSentRequest request)
      throws InvalidParamsException, InvalidRequestException {
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));
    NotifyRefundSentRequest.Refund refund = request.getRefund();

    BigDecimal totalRefunded;
    switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        Sep24Refunds sep24Refunds = txn24.getRefunds();

        if (sep24Refunds == null || sep24Refunds.getRefundPayments() == null) {
          totalRefunded =
              sum(assetInfo, refund.getAmount().getAmount(), refund.getAmountFee().getAmount());
        } else {
          if (PENDING_ANCHOR == SepTransactionStatus.from(txn.getStatus())) {
            totalRefunded =
                sum(
                    assetInfo,
                    sep24Refunds.getAmountRefunded(),
                    refund.getAmount().getAmount(),
                    refund.getAmountFee().getAmount());
          } else {
            if (refund == null) {
              totalRefunded = decimal(sep24Refunds.getAmountRefunded(), assetInfo);
            } else {
              List<Sep24RefundPayment> payments = sep24Refunds.getRefundPayments();

              // make sure refund, provided in request, was sent on refund_pending
              payments.stream()
                  .map(Sep24RefundPayment::getId)
                  .filter(id -> id.equals(refund.getId()))
                  .findFirst()
                  .orElseThrow(() -> new InvalidParamsException("Invalid refund id"));

              totalRefunded =
                  payments.stream()
                      .map(
                          payment -> {
                            if (payment.getId().equals(request.getRefund().getId())) {
                              return sum(
                                  assetInfo,
                                  refund.getAmount().getAmount(),
                                  refund.getAmountFee().getAmount());
                            } else {
                              return sum(assetInfo, payment.getAmount(), payment.getFee());
                            }
                          })
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
          }
        }
        break;
      case SEP_31:
        totalRefunded =
            sum(assetInfo, refund.getAmount().getAmount(), refund.getAmountFee().getAmount());
        break;
      default:
        throw new InvalidRequestException(
            String.format(
                "RPC method[%s] is not supported for protocol[%s]",
                getRpcMethod(), txn.getProtocol()));
    }

    BigDecimal amountIn = decimal(txn.getAmountIn(), assetInfo);
    if (totalRefunded.compareTo(amountIn) == 0) {
      return REFUNDED;
    } else if (totalRefunded.compareTo(amountIn) < 0) {
      return PENDING_ANCHOR;
    } else {
      throw new InvalidParamsException("Refund amount exceeds amount_in");
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();

    switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        switch (Kind.from(txn24.getKind())) {
          case DEPOSIT:
            if (areFundsReceived(txn24)) {
              supportedStatuses.add(PENDING_EXTERNAL);
              supportedStatuses.add(PENDING_ANCHOR);
            }
            break;
          case WITHDRAWAL:
            supportedStatuses.add(PENDING_STELLAR);
            if (areFundsReceived(txn24)) {
              supportedStatuses.add(PENDING_ANCHOR);
            }
            break;
        }
        break;
      case SEP_31:
        supportedStatuses.add(PENDING_STELLAR);
        supportedStatuses.add(PENDING_RECEIVER);
    }

    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, NotifyRefundSentRequest request) {
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));
    NotifyRefundSentRequest.Refund refund = request.getRefund();
    if (refund != null) {
      switch (PlatformTransactionData.Sep.from(txn.getProtocol())) {
        case SEP_24:
          Sep24RefundPayment sep24RefundPayment =
              JdbcSep24RefundPayment.builder()
                  .id(refund.getId())
                  .amount(refund.getAmount().getAmount())
                  .fee(refund.getAmountFee().getAmount())
                  .build();

          JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
          Sep24Refunds sep24Refunds = txn24.getRefunds();
          if (sep24Refunds == null) {
            sep24Refunds = new JdbcSep24Refunds();
          }

          if (sep24Refunds.getRefundPayments() == null) {
            sep24Refunds.setRefundPayments(List.of(sep24RefundPayment));
          } else {
            List<Sep24RefundPayment> payments = sep24Refunds.getRefundPayments();
            payments.removeIf(payment -> payment.getId().equals(request.getRefund().getId()));
            payments.add(sep24RefundPayment);
            sep24Refunds.setRefundPayments(payments);
          }

          sep24Refunds.recalculateAmounts(assetInfo);
          txn24.setRefunds(sep24Refunds);
          break;
        case SEP_31:
          RefundPayment sep31RefundPayment =
              JdbcSep31RefundPayment.builder()
                  .id(refund.getId())
                  .amount(refund.getAmount().getAmount())
                  .fee(refund.getAmountFee().getAmount())
                  .build();

          JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
          Sep31Refunds sep31Refunds = new JdbcSep31Refunds();
          sep31Refunds.setRefundPayments(List.of(sep31RefundPayment));

          sep31Refunds.recalculateAmounts(assetInfo);
          txn31.setRefunds(sep31Refunds);
          break;
      }
    }
  }
}
