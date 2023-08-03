package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_OFFCHAIN_FUNDS_PENDING;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;

import java.util.HashSet;
import java.util.Set;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyOffchainFundsPendingRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyOffchainFundsPendingHandler
    extends ActionHandler<NotifyOffchainFundsPendingRequest> {

  public NotifyOffchainFundsPendingHandler(
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
        NotifyOffchainFundsPendingRequest.class);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_OFFCHAIN_FUNDS_PENDING;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOffchainFundsPendingRequest request)
      throws InvalidRequestException {
    return PENDING_EXTERNAL;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    switch (Sep.from(txn.getProtocol())) {
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (WITHDRAWAL == Kind.from(txn24.getKind())) {
          if (areFundsReceived(txn)) {
            return Set.of(PENDING_ANCHOR);
          }
        }
        break;
      case SEP_31:
        supportedStatuses.add(PENDING_RECEIVER);
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOffchainFundsPendingRequest request) {
    if (request.getExternalTransactionId() != null) {
      txn.setExternalTransactionId(request.getExternalTransactionId());
    }
  }
}
