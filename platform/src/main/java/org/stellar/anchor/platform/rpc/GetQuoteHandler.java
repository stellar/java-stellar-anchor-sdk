package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.rpc.method.RpcMethod.GET_QUOTE;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.GetQuoteResponse;
import org.stellar.anchor.api.rpc.method.GetQuoteRpcRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.shared.StellarId;
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
    return GetQuoteResponse.builder()
        .id(quote.getId())
        .expiresAt(quote.getExpiresAt())
        .price(quote.getPrice())
        .sellAsset(quote.getSellAsset())
        .sellAmount(quote.getSellAmount())
        .sellDeliveryMethod(quote.getSellDeliveryMethod())
        .buyAsset(quote.getBuyAsset())
        .buyAmount(quote.getBuyAmount())
        .buyDeliveryMethod(quote.getBuyDeliveryMethod())
        .fee(quote.getFee())
        .totalPrice(quote.getTotalPrice())
        .transactionId(quote.getTransactionId())
        .createdAt(quote.getCreatedAt())
        .creator(new StellarId(null, quote.getCreatorAccountId(), quote.getCreatorMemo()))
        .build();
  }
}
