package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class QuoteEvent implements AnchorEvent {
  @SerializedName("event_id")
  String eventId;

  Type type;
  public String getType(){
    return this.type.type;
  }

  String id;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("expires_at")
  Instant expiresAt;

  String price;

  StellarId creator;

  @SerializedName("transaction_id")
  String transactionId;

  @SerializedName("created_at")
  Instant createdAt;

  public enum Type {
    QUOTE_CREATED("quote_created");

    @JsonValue
    public final String type;

    Type(String type){
      this.type = type;
    }
  }

  public QuoteEvent(){}
}
