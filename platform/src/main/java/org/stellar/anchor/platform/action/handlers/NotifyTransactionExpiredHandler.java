package org.stellar.anchor.platform.action.handlers;

import static java.util.stream.Collectors.toSet;
import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.EXPIRED;
import static org.stellar.anchor.platform.action.dto.ActionMethod.NOTIFY_TRANSACTION_EXPIRED;

import java.util.Arrays;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.action.dto.ActionMethod;
import org.stellar.anchor.platform.action.dto.NotifyTransactionExpiredRequest;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyTransactionExpiredHandler
    extends ActionHandler<NotifyTransactionExpiredRequest> {

  public NotifyTransactionExpiredHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRANSACTION_EXPIRED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {
    return EXPIRED;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    return Arrays.stream(SepTransactionStatus.values())
        .filter(s -> s != ERROR && s != EXPIRED)
        .collect(toSet());
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24", "31");
  }

  @Override
  protected boolean isMessageRequired() {
    return true;
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTransactionExpiredRequest request) {}
}
