package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_SENT;
import static org.stellar.anchor.api.sep.SepTransactionStatus.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_STELLAR;
import static org.stellar.anchor.platform.utils.PaymentsUtil.addStellarTransaction;
import static org.stellar.anchor.util.Log.errorEx;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.rpc.InternalErrorException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsSentRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.sdk.responses.operations.OperationResponse;

public class NotifyOnchainFundsSentHandler extends ActionHandler<NotifyOnchainFundsSentRequest> {

  private final Horizon horizon;

  public NotifyOnchainFundsSentHandler(
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
        NotifyOnchainFundsSentRequest.class);
    this.horizon = horizon;
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_ONCHAIN_FUNDS_SENT;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOnchainFundsSentRequest request)
      throws InvalidRequestException {
    return COMPLETED;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (DEPOSIT == Kind.from(txn24.getKind())) {
        supportedStatuses.add(PENDING_STELLAR);
        if (areFundsReceived(txn24)) {
          supportedStatuses.add(PENDING_ANCHOR);
        }
      }
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOnchainFundsSentRequest request) throws InternalErrorException {
    Instant transferReceivedAt = txn.getTransferReceivedAt();

    String stellarTxnId = request.getStellarTransactionId();
    try {
      List<OperationResponse> txnOperations = horizon.getStellarTxnOperations(stellarTxnId);
      addStellarTransaction(txn, stellarTxnId, txnOperations);
    } catch (IOException ex) {
      errorEx(String.format("Failed to retrieve stellar transaction by ID[%s]", stellarTxnId), ex);
      throw new InternalErrorException(
          String.format("Failed to retrieve Stellar transaction by ID[%s]", stellarTxnId), ex);
    }

    txn.setTransferReceivedAt(transferReceivedAt);
  }
}
