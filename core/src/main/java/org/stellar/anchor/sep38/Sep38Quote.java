package org.stellar.anchor.sep38;

import java.time.Instant;

public interface Sep38Quote {
  String getId();

  void setId(String id);

  Instant getExpiresAt();

  void setExpiresAt(Instant expiresAt);

  String getPrice();

  void setPrice(String price);

  String getSellAsset();

  void setSellAsset(String sellAsset);

  String getSellAmount();

  void setSellAmount(String sellAmount);

  String getSellDeliveryMethod();

  void setSellDeliveryMethod(String sellDeliveryMethod);

  String getBuyAsset();

  void setBuyAsset(String buyAsset);

  String getBuyAmount();

  void setBuyAmount(String buyAmount);

  String getBuyDeliveryMethod();

  void setBuyDeliveryMethod(String buyDeliveryMethod);

  Instant getCreatedAt();

  void setCreatedAt(Instant createdAt);

  String getCreatorAccountId();

  void setCreatorAccountId(String creatorAccountId);

  String getCreatorMemo();

  void setCreatorMemo(String creatorMemo);

  String getCreatorMemoType();

  void setCreatorMemoType(String creatorMemoType);

  String getTransactionId();

  void setTransactionId(String transactionId);
}
