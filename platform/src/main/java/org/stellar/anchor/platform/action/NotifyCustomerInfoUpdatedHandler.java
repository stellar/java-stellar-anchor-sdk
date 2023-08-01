package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_31;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_CUSTOMER_INFO_UPDATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;

import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyCustomerInfoUpdatedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyCustomerInfoUpdatedHandler
    extends ActionHandler<NotifyCustomerInfoUpdatedRequest> {

  public NotifyCustomerInfoUpdatedHandler(
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
        NotifyCustomerInfoUpdatedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyCustomerInfoUpdatedRequest request)
      throws InvalidRequestException, InvalidParamsException, BadRequestException {
    super.validate(txn, request);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_CUSTOMER_INFO_UPDATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyCustomerInfoUpdatedRequest request) {
    return PENDING_RECEIVER;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (SEP_31 == Sep.from(txn.getProtocol())) {
      return Set.of(PENDING_CUSTOMER_INFO_UPDATE);
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyCustomerInfoUpdatedRequest request) {}
}
