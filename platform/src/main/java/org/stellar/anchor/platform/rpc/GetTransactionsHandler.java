package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_TRANSACTIONS;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.rpc.method.GetTransactionsRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.util.TransactionsParams;

public class GetTransactionsHandler extends RpcMethodHandler<GetTransactionsRpcRequest> {
  private final TransactionService txnService;

  public GetTransactionsHandler(TransactionService txnService) {
    this.txnService = txnService;
  }

  public RpcMethod getRpcMethod() {
    return GET_TRANSACTIONS;
  }

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
    return txnService.findTransactions(request.getSep(), params);
  }
}
