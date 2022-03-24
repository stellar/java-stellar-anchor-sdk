package org.stellar.anchor.model;

import java.time.LocalDateTime;

public interface Sep38Quote {
  String getId();

  void setId(String id);

  LocalDateTime getExpiresAt();

  void setExpiresAt(LocalDateTime expiresAt);

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

  LocalDateTime getCreatedAt();

  void setCreatedAt(LocalDateTime createdAt);

  String getCreatorAccountId();

  void setCreatorAccountId(String creatorAccountId);

  String getCreatorMemo();

  void setCreatorMemo(String creatorMemo);

  String getCreatorMemoType();

  void setCreatorMemoType(String creatorMemoType);

  String getTransactionId();

  void setTransactionId(String transactionId);
}
