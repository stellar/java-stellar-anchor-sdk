package org.stellar.anchor.platform.rpc;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31;
import static org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_TRANSACTION_EXPIRED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;

import java.util.Arrays;
import java.util.Set;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.NotifyTransactionExpiredRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTransactionExpiredHandler
    extends RpcMethodHandler<NotifyTransactionExpiredRequest> {

  private final JdbcTransactionPendingTrustRepo transactionPendingTrustRepo;

  public NotifyTransactionExpiredHandler(
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
        NotifyTransactionExpiredRequest.class);
    this.transactionPendingTrustRepo = transactionPendingTrustRepo;
  }

  @Override
  public RpcMethod getRpcMethod() {
    return NOTIFY_TRANSACTION_EXPIRED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {
    return EXPIRED;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (Set.of(SEP_24, SEP_31).contains(Sep.from(txn.getProtocol()))) {
      if (!areFundsReceived(txn)) {
        return Arrays.stream(SepTransactionStatus.values())
            .filter(s -> !isErrorStatus(s) && !isFinalStatus(s))
            .collect(toSet());
      }
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {
    if (transactionPendingTrustRepo.existsById(txn.getId())) {
      transactionPendingTrustRepo.deleteById(txn.getId());
    }
  }
}
