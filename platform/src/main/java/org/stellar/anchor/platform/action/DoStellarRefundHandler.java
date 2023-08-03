package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31;
import static org.stellar.anchor.api.rpc.action.ActionMethod.DO_STELLAR_REFUND;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_STELLAR;
import static org.stellar.anchor.util.AssetHelper.getAssetCode;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.sum;

import java.math.BigDecimal;
import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.DoStellarRefundRequest;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31Refunds;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class DoStellarRefundHandler extends ActionHandler<DoStellarRefundRequest> {

  private final CustodyService custodyService;
  private final CustodyConfig custodyConfig;

  public DoStellarRefundHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      CustodyConfig custodyConfig,
      AssetService assetService,
      CustodyService custodyService,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        DoStellarRefundRequest.class);
    this.custodyService = custodyService;
    this.custodyConfig = custodyConfig;
  }

  @Override
  protected void validate(JdbcSepTransaction txn, DoStellarRefundRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    if (!custodyConfig.isCustodyIntegrationEnabled()) {
      throw new InvalidParamsException(
          String.format("Action[%s] requires enabled custody integration", getActionType()));
    }

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
  public ActionMethod getActionType() {
    return DO_STELLAR_REFUND;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, DoStellarRefundRequest request)
      throws InvalidRequestException, InvalidParamsException {
    String amount = request.getRefund().getAmount().getAmount();
    String amountFee = request.getRefund().getAmountFee().getAmount();
    AssetInfo assetInfo = assetService.getAsset(getAssetCode(txn.getAmountInAsset()));

    BigDecimal totalRefunded;
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        Sep24Refunds sep24Refunds = txn24.getRefunds();
        if (sep24Refunds == null || sep24Refunds.getRefundPayments() == null) {
          totalRefunded = sum(assetInfo, amount, amountFee);
        } else {
          totalRefunded = sum(assetInfo, sep24Refunds.getAmountRefunded(), amount, amountFee);
        }
        break;
      case SEP_31:
        JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
        Sep31Refunds sep31Refunds = txn31.getRefunds();
        if (sep31Refunds == null || sep31Refunds.getRefundPayments() == null) {
          totalRefunded = sum(assetInfo, amount, amountFee);
        } else {
          throw new InvalidRequestException(
              String.format(
                  "Multiple refunds aren't supported for kind[%s], protocol[%s] and action[%s]",
                  RECEIVE, txn.getProtocol(), getActionType()));
        }
        break;
      default:
        throw new InvalidRequestException(
            String.format(
                "Action[%s] is not supported for protocol[%s]",
                getActionType(), txn.getProtocol()));
    }

    BigDecimal amountIn = decimal(txn.getAmountIn(), assetInfo);
    if (totalRefunded.compareTo(amountIn) > 0) {
      throw new InvalidParamsException("Refund amount exceeds amount_in");
    } else if (SEP_31 == Sep.from(txn.getProtocol()) && totalRefunded.compareTo(amountIn) < 0) {
      throw new InvalidParamsException("Refund amount is less than amount_in");
    }
    return PENDING_STELLAR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (WITHDRAWAL == Kind.from(txn24.getKind())) {
          if (areFundsReceived(txn24)) {
            return Set.of(PENDING_ANCHOR);
          }
        }
        break;
      case SEP_31:
        return Set.of(PENDING_RECEIVER);
    }

    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(JdbcSepTransaction txn, DoStellarRefundRequest request)
      throws AnchorException {
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        custodyService.createTransactionRefund(
            request, txn24.getRefundMemo(), txn24.getRefundMemoType());
        break;
      case SEP_31:
        JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
        custodyService.createTransactionRefund(
            request, txn31.getStellarMemo(), txn31.getStellarMemoType());
        break;
    }
  }
}
