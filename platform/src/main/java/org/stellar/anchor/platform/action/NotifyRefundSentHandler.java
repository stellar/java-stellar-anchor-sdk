package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_REFUND_SENT;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
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
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.NotifyRefundSentRequest;
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

public class NotifyRefundSentHandler extends ActionHandler<NotifyRefundSentRequest> {

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

    if (request.getRefund() == null
        && PENDING_ANCHOR == SepTransactionStatus.from(txn.getStatus())) {
      throw new InvalidParamsException("refund is required");
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
  public ActionMethod getActionType() {
    return NOTIFY_REFUND_SENT;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyRefundSentRequest request) throws InvalidParamsException {

    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));
    Sep24Refunds sep24Refunds = txn24.getRefunds();
    NotifyRefundSentRequest.Refund refund = request.getRefund();

    BigDecimal totalRefunded;
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
      } else { // PENDING_EXTERNAL
        if (request.getRefund() == null) {
          totalRefunded = decimal(sep24Refunds.getAmountRefunded(), assetInfo);
        } else {
          List<Sep24RefundPayment> payments = sep24Refunds.getRefundPayments();

          // make sure refund, provided in request, was sent on refund_pending
          payments.stream()
              .map(Sep24RefundPayment::getId)
              .filter(id -> id.equals(request.getRefund().getId()))
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

    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyRefundSentRequest request) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    NotifyRefundSentRequest.Refund refund = request.getRefund();
    if (refund != null) {
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
        sep24Refunds.setRefundPayments(List.of(refundPayment));
      } else {
        List<Sep24RefundPayment> payments = sep24Refunds.getRefundPayments();
        payments.removeIf(payment -> payment.getId().equals(request.getRefund().getId()));
        payments.add(refundPayment);
        sep24Refunds.setRefundPayments(payments);
      }

      AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));
      sep24Refunds.recalculateAmounts(assetInfo);
      txn24.setRefunds(sep24Refunds);
    }
  }
}
