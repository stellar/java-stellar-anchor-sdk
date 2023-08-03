package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_RECOVERY;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;

import java.util.Set;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTransactionRecoveryRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTransactionRecoveryHandler
    extends ActionHandler<NotifyTransactionRecoveryRequest> {

  public NotifyTransactionRecoveryHandler(
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
        NotifyTransactionRecoveryRequest.class);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRANSACTION_RECOVERY;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionRecoveryRequest request)
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
    if (Set.of(SEP_24, SEP_31).contains(Sep.from(txn.getProtocol()))) {
      if (areFundsReceived(txn)) {
        return Set.of(ERROR, EXPIRED);
      }
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTransactionRecoveryRequest request)
      throws InvalidParamsException {}
}
