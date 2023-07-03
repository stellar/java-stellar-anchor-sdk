package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.rpc.ActionMethod.NOTIFY_REFUND_INITIATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_STELLAR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.REFUNDED;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.ActionMethod;
import org.stellar.anchor.api.rpc.NotifyRefundSentRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyRefundSentHandler extends ActionHandler<NotifyRefundSentRequest> {

  public NotifyRefundSentHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_REFUND_INITIATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyRefundSentRequest request) {
    // TODO: Check whether refund is completed
    if (true) {
      return REFUNDED;
    } else {
      return PENDING_ANCHOR;
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();

    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    switch (Kind.from(txn24.getKind())) {
      case DEPOSIT:
        if (txn24.getTransferReceivedAt() != null) {
          supportedStatuses.add(PENDING_EXTERNAL);
          supportedStatuses.add(PENDING_ANCHOR);
        }
        break;
      case WITHDRAWAL:
        supportedStatuses.add(PENDING_STELLAR);
        if (txn24.getTransferReceivedAt() != null) {
          supportedStatuses.add(PENDING_ANCHOR);
        }
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
      JdbcSepTransaction txn, NotifyRefundSentRequest request) {
    // TODO: add refunds
  }
}
