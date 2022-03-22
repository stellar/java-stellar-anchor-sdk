package org.stellar.anchor.model;

import java.time.LocalDateTime;
import org.stellar.anchor.sep38.Sep38QuoteStore;

@SuppressWarnings("unused")
public class Sep38QuoteBuilder {
  final Sep38Quote quote;

  public Sep38QuoteBuilder(Sep38QuoteStore factory) {
    quote = factory.newInstance();
  }

  public Sep38QuoteBuilder id(String id) {
    quote.setId(id);
    return this;
  }

  public Sep38QuoteBuilder expiresAt(LocalDateTime expiresAt) {
    quote.setExpiresAt(expiresAt);
    return this;
  }

  public Sep38QuoteBuilder price(String price) {
    quote.setPrice(price);
    return this;
  }

  public Sep38QuoteBuilder sellAsset(String sellAsset) {
    quote.setSellAsset(sellAsset);
    return this;
  }

  public Sep38QuoteBuilder sellAmount(String sellAmount) {
    quote.setSellAmount(sellAmount);
    return this;
  }

  public Sep38QuoteBuilder sellDeliveryMethod(String sellDeliveryMethod) {
    quote.setSellDeliveryMethod(sellDeliveryMethod);
    return this;
  }

  public Sep38QuoteBuilder buyAsset(String buyAsset) {
    quote.setBuyAsset(buyAsset);
    return this;
  }

  public Sep38QuoteBuilder buyAmount(String buyAmount) {
    quote.setBuyAmount(buyAmount);
    return this;
  }

  public Sep38QuoteBuilder buyDeliveryMethod(String buyDeliveryMethod) {
    quote.setBuyDeliveryMethod(buyDeliveryMethod);
    return this;
  }

  public Sep38QuoteBuilder createdAt(LocalDateTime createdAt) {
    quote.setCreatedAt(createdAt);
    return this;
  }

  public Sep38QuoteBuilder creatorAccountId(String creatorAccountId) {
    quote.setCreatorAccountId(creatorAccountId);
    return this;
  }

  public Sep38QuoteBuilder creatorMemo(String creatorMemo) {
    quote.setCreatorMemo(creatorMemo);
    return this;
  }

  public Sep38QuoteBuilder transactionId(String transactionId) {
    quote.setTransactionId(transactionId);
    return this;
  }

  public Sep38Quote build() {
    return quote;
  }
}
