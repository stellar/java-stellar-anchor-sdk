package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.ActionMethod.NOTIFY_OFFCHAIN_FUNDS_AVAILABLE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;

import java.time.Instant;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.ActionMethod;
import org.stellar.anchor.api.rpc.NotifyOffchainFundsAvailableRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyOffchainFundsAvailableHandler
    extends ActionHandler<NotifyOffchainFundsAvailableRequest> {

  public NotifyOffchainFundsAvailableHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_OFFCHAIN_FUNDS_AVAILABLE;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOffchainFundsAvailableRequest request) {
    if ("24".equals(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (Kind.from(txn24.getKind()) == Kind.WITHDRAWAL) {
        return COMPLETED;
      }
      throw new IllegalArgumentException(
          String.format(
              "Invalid kind[%s] for protocol[%s] and action[%s]",
              txn24.getKind(), txn24.getProtocol(), getActionType()));
    }
    throw new IllegalArgumentException(
        String.format("Invalid protocol[%s] for action[%s]", txn.getProtocol(), getActionType()));
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (Kind.from(txn24.getKind()) == Kind.WITHDRAWAL) {
      if (txn24.getTransferReceivedAt() != null) {
        return Set.of(PENDING_ANCHOR);
      }
    }
    return Set.of();
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOffchainFundsAvailableRequest request) {
    txn.setExternalTransactionId(request.getExternalTransactionId());
    if (request.getFundsReceivedAt() == null) {
      txn.setTransferReceivedAt(Instant.now());
    } else {
      txn.setTransferReceivedAt(request.getFundsReceivedAt());
    }
  }
}
