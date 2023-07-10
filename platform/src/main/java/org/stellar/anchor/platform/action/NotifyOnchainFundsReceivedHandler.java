package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_RECEIVED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsReceivedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyOnchainFundsReceivedHandler
    extends ActionHandler<NotifyOnchainFundsReceivedRequest> {

  public NotifyOnchainFundsReceivedHandler(
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
        NotifyOnchainFundsReceivedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    if (!((request.getAmountIn() == null
            && request.getAmountOut() == null
            && request.getAmountFee() == null)
        || (request.getAmountIn() != null
            && request.getAmountOut() != null
            && request.getAmountFee() != null)
        || (request.getAmountIn() != null
            && request.getAmountOut() == null
            && request.getAmountFee() == null))) {
      throw new InvalidParamsException(
          "Invalid amounts combination provided: all, none or only amount_in should be set");
    }

    if (request.getAmountIn() != null) {
      AssetValidationUtils.validateAsset(
          "amount_in",
          AmountAssetRequest.builder()
              .amount(request.getAmountIn().getAmount())
              .asset(txn.getAmountInAsset())
              .build(),
          assetService);
    }
    if (request.getAmountOut() != null) {
      AssetValidationUtils.validateAsset(
          "amount_out",
          AmountAssetRequest.builder()
              .amount(request.getAmountOut().getAmount())
              .asset(txn.getAmountOutAsset())
              .build(),
          assetService);
    }
    if (request.getAmountFee() != null) {
      AssetValidationUtils.validateAsset(
          "amount_fee",
          AmountAssetRequest.builder()
              .amount(request.getAmountFee().getAmount())
              .asset(txn.getAmountFeeAsset())
              .build(),
          true,
          assetService);
    }
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_ONCHAIN_FUNDS_RECEIVED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request)
      throws InvalidRequestException {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (WITHDRAWAL == Kind.from(txn24.getKind())) {
      return PENDING_ANCHOR;
    }
    throw new InvalidRequestException(
        String.format(
            "Invalid kind[%s] for protocol[%s] and action[%s]",
            txn24.getKind(), txn24.getProtocol(), getActionType()));
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (WITHDRAWAL == Kind.from(txn24.getKind())) {
        supportedStatuses.add(PENDING_USR_TRANSFER_START);
      }
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request) {
    addStellarTransaction(txn, request.getStellarTransactionId());

    if (request.getAmountIn() != null) {
      txn.setAmountIn(request.getAmountIn().getAmount());
    }
    if (request.getAmountOut() != null) {
      txn.setAmountOut(request.getAmountOut().getAmount());
    }
    if (request.getAmountFee() != null) {
      txn.setAmountFee(request.getAmountFee().getAmount());
    }
  }
}
