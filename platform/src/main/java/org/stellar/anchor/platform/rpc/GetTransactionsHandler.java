package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_TRANSACTIONS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.GetTransactionsResponse;
import org.stellar.anchor.api.platform.TransactionsSeps;
import org.stellar.anchor.api.rpc.method.GetTransactionsRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.PlatformTransactionHelper;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;
import org.stellar.anchor.util.TransactionsParams;

public class GetTransactionsHandler extends RpcMethodHandler<GetTransactionsRpcRequest> {

  public GetTransactionsHandler(
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
        GetTransactionsRpcRequest.class);
  }

  @Override
  public Object handle(Object requestParams) throws AnchorException {
    GetTransactionsRpcRequest request =
        gson.fromJson(gson.toJson(requestParams), GetTransactionsRpcRequest.class);
    TransactionsParams params =
        new TransactionsParams(
            request.getOrderBy(),
            request.getOrder(),
            request.getStatuses(),
            request.getPageNumber(),
            request.getPageSize());
    return findTransactions(request.getSep(), params);
  }

  @Override
  public RpcMethod getRpcMethod() {
    return GET_TRANSACTIONS;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, GetTransactionsRpcRequest request)
      throws InvalidRequestException, InvalidParamsException {
    return null;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    return null;
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, GetTransactionsRpcRequest request) throws AnchorException {}

  private GetTransactionsResponse findTransactions(TransactionsSeps sep, TransactionsParams params)
      throws AnchorException {
    List<?> txn;
    switch (sep) {
      case SEP_6:
        txn = txn6Store.findTransactions(params);
        break;
      case SEP_24:
        txn = txn24Store.findTransactions(params);
        break;
      case SEP_31:
        txn = txn31Store.findTransactions(params);
        break;
      default:
        throw new BadRequestException("SEP not supported");
    }
    return new GetTransactionsResponse(
        txn.stream()
            .map(
                t ->
                    PlatformTransactionHelper.toGetTransactionResponse(
                        (JdbcSepTransaction) t, assetService))
            .collect(Collectors.toList()));
  }
}
