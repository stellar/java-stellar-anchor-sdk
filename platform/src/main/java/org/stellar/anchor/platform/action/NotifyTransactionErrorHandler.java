package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;

import java.util.Arrays;
import java.util.Set;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTransactionErrorRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTransactionErrorHandler extends ActionHandler<NotifyTransactionErrorRequest> {

  private final JdbcTransactionPendingTrustRepo transactionPendingTrustRepo;

  public NotifyTransactionErrorHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        NotifyTransactionErrorRequest.class);
    this.transactionPendingTrustRepo = transactionPendingTrustRepo;
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRANSACTION_ERROR;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionErrorRequest request) {
    return ERROR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (Set.of(SEP_24, SEP_31).contains(Sep.from(txn.getProtocol()))) {
      return Arrays.stream(SepTransactionStatus.values())
          .filter(s -> !isErrorStatus(s) && !isFinalStatus(s))
          .collect(toSet());
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTransactionErrorRequest request) {
    if (transactionPendingTrustRepo.existsById(txn.getId())) {
      transactionPendingTrustRepo.deleteById(txn.getId());
    }
  }
}
