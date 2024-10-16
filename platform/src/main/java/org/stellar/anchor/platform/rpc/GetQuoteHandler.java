package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_QUOTE;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.rpc.method.GetQuoteRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;

public class GetQuoteHandler extends RpcMethodHandler<GetQuoteRpcRequest> {
  final Sep38QuoteStore sep38QuoteStore;

  public GetQuoteHandler(Sep38QuoteStore sep38QuoteStore) {
    this.sep38QuoteStore = sep38QuoteStore;
  }

  @Override
  public RpcMethod getRpcMethod() {
    return GET_QUOTE;
  }

  @Override
  public Object handle(Object requestParams) throws AnchorException {
    GetQuoteRpcRequest request =
        gson.fromJson(gson.toJson(requestParams), GetQuoteRpcRequest.class);
    Sep38Quote quote = sep38QuoteStore.findByQuoteId(request.getQuoteId());
    return quote;
    //    GetQuoteResponse response = new GetQuoteResponse(quote);
  }
}
