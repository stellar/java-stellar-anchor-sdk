package org.stellar.anchor.platform.action.handlers;

import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_TRUST;
import static org.stellar.anchor.platform.action.dto.ActionMethod.NOTIFY_TRUST_SET;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.action.dto.ActionMethod;
import org.stellar.anchor.platform.action.dto.NotifyTrustSetRequest;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyTrustSetHandler extends ActionHandler<NotifyTrustSetRequest> {

  public NotifyTrustSetHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRUST_SET;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTrustSetRequest request) {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();

    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        switch (Kind.from(txn24.getKind())) {
          case DEPOSIT:
            supportedStatuses.add(PENDING_TRUST);
            break;
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
  protected boolean isMessageRequired() {
    return false;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTrustSetRequest request) {}
}
