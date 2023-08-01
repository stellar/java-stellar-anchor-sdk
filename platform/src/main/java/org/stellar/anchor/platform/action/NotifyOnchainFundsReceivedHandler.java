package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_RECEIVED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_SENDER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.platform.utils.PaymentsUtil.addStellarTransaction;
import static org.stellar.anchor.util.Log.errorEx;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InternalErrorException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountAssetRequest;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsReceivedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.sdk.responses.operations.OperationResponse;

public class NotifyOnchainFundsReceivedHandler
    extends ActionHandler<NotifyOnchainFundsReceivedRequest> {

  private final Horizon horizon;

  public NotifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      Horizon horizon,
      AssetService assetService,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        NotifyOnchainFundsReceivedRequest.class);
    this.horizon = horizon;
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
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        return PENDING_ANCHOR;
      case SEP_31:
        return PENDING_RECEIVER;
      default:
        throw new InvalidRequestException(
            String.format(
                "Action[%s] is not supported for protocol[%s]",
                getActionType(), txn.getProtocol()));
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (WITHDRAWAL == Kind.from(txn24.getKind())) {
          supportedStatuses.add(PENDING_USR_TRANSFER_START);
        }
        break;
      case SEP_31:
        supportedStatuses.add(PENDING_SENDER);
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request) throws AnchorException {
    String stellarTxnId = request.getStellarTransactionId();
    try {
      List<OperationResponse> txnOperations = horizon.getStellarTxnOperations(stellarTxnId);
      addStellarTransaction(txn, stellarTxnId, txnOperations);
    } catch (IOException ex) {
      errorEx(String.format("Failed to retrieve stellar transaction by ID[%s]", stellarTxnId), ex);
      throw new InternalErrorException(
          String.format("Failed to retrieve Stellar transaction by ID[%s]", stellarTxnId), ex);
    }

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
