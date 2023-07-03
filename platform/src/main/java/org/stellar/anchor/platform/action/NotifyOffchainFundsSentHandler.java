package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_OFFCHAIN_FUNDS_SENT;
import static org.stellar.anchor.api.sep.SepTransactionStatus.COMPLETED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyOffchainFundsSentRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyOffchainFundsSentHandler extends ActionHandler<NotifyOffchainFundsSentRequest> {

  public NotifyOffchainFundsSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_OFFCHAIN_FUNDS_SENT;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOffchainFundsSentRequest request) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    switch (Kind.from(txn24.getKind())) {
      case DEPOSIT:
        return PENDING_EXTERNAL;
      case WITHDRAWAL:
        return COMPLETED;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Invalid kind[%s] for protocol[%s] and action[%s]",
                txn24.getKind(), txn24.getProtocol(), getActionType()));
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    switch (Kind.from(txn24.getKind())) {
      case DEPOSIT:
        supportedStatuses.add(PENDING_USR_TRANSFER_START);
        break;
      case WITHDRAWAL:
        supportedStatuses.add(PENDING_ANCHOR);
        break;
    }
    return supportedStatuses;
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOffchainFundsSentRequest request) {
    txn.setExternalTransactionId(request.getExternalTransactionId());
    if (request.getFundsReceivedAt() == null) {
      txn.setTransferReceivedAt(Instant.now());
    } else {
      txn.setTransferReceivedAt(request.getFundsReceivedAt());
    }
  }
}
