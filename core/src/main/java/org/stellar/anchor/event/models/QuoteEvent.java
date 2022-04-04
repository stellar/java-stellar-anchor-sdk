package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuoteEvent implements AnchorEvent {
  String id;

  String type;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("expires_at")
  LocalDateTime expiresAt;

  String price;

  StellarId creator;

  @SerializedName("transaction_id")
  String transactionId;

  @SerializedName("created_at")
  LocalDateTime createdAt;

  @SerializedName("client_domain")
  String clientDomain;

  public QuoteEvent() {}

  public QuoteEvent(
      String id,
      String type,
      String sellAsset,
      String buyAsset,
      LocalDateTime expiresAt,
      String price,
      StellarId creator,
      String transactionId,
      LocalDateTime createdAt,
      String clientDomain) {
    this.id = id;
    this.type = type;
    this.sellAsset = sellAsset;
    this.buyAsset = buyAsset;
    this.expiresAt = expiresAt;
    this.price = price;
    this.creator = creator;
    this.transactionId = transactionId;
    this.createdAt = createdAt;
    this.clientDomain = clientDomain;
  }
}
