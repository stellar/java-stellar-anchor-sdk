package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_TRANSACTION;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.rpc.method.GetTransactionRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.platform.service.TransactionService;

public class GetTransactionHandler extends RpcMethodHandler<GetTransactionRpcRequest> {
  private final TransactionService txnService;

  public GetTransactionHandler(TransactionService txnService) {
    this.txnService = txnService;
  }

  public RpcMethod getRpcMethod() {
    return GET_TRANSACTION;
  }

  public Object handle(Object requestParams) throws AnchorException {
    GetTransactionRpcRequest request =
        gson.fromJson(gson.toJson(requestParams), GetTransactionRpcRequest.class);
    return txnService.findTransaction(request.getTransactionId());
  }
}
