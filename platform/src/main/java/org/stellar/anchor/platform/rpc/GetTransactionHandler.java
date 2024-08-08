package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_TRANSACTION;
import static org.stellar.anchor.platform.utils.PlatformTransactionHelper.toGetTransactionResponse;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.GetTransactionRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.SepHelper;

public class GetTransactionHandler extends RpcMethodHandler<GetTransactionRpcRequest> {
  public GetTransactionHandler(
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
        GetTransactionRpcRequest.class);
  }

  @Override
  public Object handle(Object requestParams) throws AnchorException {
    GetTransactionRpcRequest request =
        gson.fromJson(gson.toJson(requestParams), GetTransactionRpcRequest.class);
    if (isEmpty(request.getTransactionId())) {
      Log.info("Rejecting GET /transaction/:id because the id is empty.");
      throw new InvalidParamsException("transaction id cannot be empty");
    }

    Log.infoF("Processing RPC request {}", request);
    JdbcSepTransaction txn = getTransaction(request.getTransactionId());
    if (txn == null) {
      throw new InvalidRequestException(
          String.format("Transaction with id[%s] is not found", request.getTransactionId()));
    }

    return toGetTransactionResponse(txn, assetService);
  }

  @Override
  public RpcMethod getRpcMethod() {
    return GET_TRANSACTION;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, GetTransactionRpcRequest request)
      throws InvalidRequestException, InvalidParamsException {
    return null;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    switch (Sep.from((txn.getProtocol()))) {
      case SEP_6:
        return SepHelper.sep6Statuses;
      case SEP_24:
        return SepHelper.sep24Statuses;
      case SEP_31:
        return SepHelper.sep31Statuses;
      default:
        throw new IllegalStateException("Unsupported protocol: " + txn.getProtocol());
    }
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, GetTransactionRpcRequest request) throws AnchorException {}
}
