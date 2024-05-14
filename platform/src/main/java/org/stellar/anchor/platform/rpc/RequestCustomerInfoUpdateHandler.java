package org.stellar.anchor.platform.rpc;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_6;
import static org.stellar.anchor.api.rpc.method.RpcMethod.REQUEST_CUSTOMER_INFO_UPDATE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;

import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.RequestCustomerInfoUpdateRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6Transaction;
import org.stellar.anchor.sep6.Sep6TransactionStore;

public class RequestCustomerInfoUpdateHandler
    extends RpcMethodHandler<RequestCustomerInfoUpdateRequest> {

  public RequestCustomerInfoUpdateHandler(
      Sep6TransactionStore txn6Store,
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    super(
        txn6Store,
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
        RequestCustomerInfoUpdateRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, RequestCustomerInfoUpdateRequest request)
      throws InvalidRequestException, InvalidParamsException, BadRequestException {
    super.validate(txn, request);
  }

  @Override
  public RpcMethod getRpcMethod() {
    return REQUEST_CUSTOMER_INFO_UPDATE;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, RequestCustomerInfoUpdateRequest request) {
    return PENDING_CUSTOMER_INFO_UPDATE;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    switch (Sep.from(txn.getProtocol())) {
      case SEP_6:
        return Set.of(INCOMPLETE, PENDING_ANCHOR, PENDING_CUSTOMER_INFO_UPDATE);
      case SEP_31:
        return Set.of(PENDING_RECEIVER);
      default:
        return emptySet();
    }
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, RequestCustomerInfoUpdateRequest request) {
    if (Sep.from(txn.getProtocol()) == SEP_6) {
      Sep6Transaction txn6 = (Sep6Transaction) txn;

      if (request.getRequiredCustomerInfoMessage() != null) {
        txn6.setRequiredCustomerInfoMessage(request.getRequiredCustomerInfoMessage());
      }

      if (request.getRequiredCustomerInfoUpdates() != null) {
        txn6.setRequiredCustomerInfoUpdates(request.getRequiredCustomerInfoUpdates());
      }
    }

    if (request.getUserActionRequiredBy() != null) {
      txn.setUserActionRequiredBy(request.getUserActionRequiredBy());
    }
  }
}
