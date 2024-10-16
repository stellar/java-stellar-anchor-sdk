package org.stellar.anchor.sep38;

import org.stellar.anchor.api.sep.sep38.Sep38QuoteResponse;

public class Sep38Helper {
  public static Sep38QuoteResponse sep38QuoteResponseFromQuote(Sep38Quote quote) {
    return Sep38QuoteResponse.builder()
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
        .build();
  }
}
