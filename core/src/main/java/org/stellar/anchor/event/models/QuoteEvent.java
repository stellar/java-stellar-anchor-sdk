package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.api.shared.StellarId;

@Data
@Builder
@AllArgsConstructor
public class QuoteEvent implements AnchorEvent {
  @JsonProperty("event_id")
  @SerializedName("event_id")
  String eventId;

  Type type;

  public String getType() {
    return this.type.type;
  }

  String id;

  @JsonProperty("sell_asset")
  @SerializedName("sell_asset")
  String sellAsset;

  @JsonProperty("sell_amount")
  @SerializedName("sell_amount")
  String sellAmount;

  @JsonProperty("buy_asset")
  @SerializedName("buy_asset")
  String buyAsset;

  @JsonProperty("buy_amount")
  @SerializedName("buy_amount")
  String buyAmount;

  @JsonProperty("expires_at")
  @SerializedName("expires_at")
  Instant expiresAt;

  String price;

  @JsonProperty("total_price")
  @SerializedName("total_price")
  String totalPrice;

  StellarId creator;

  @JsonProperty("transaction_id")
  @SerializedName("transaction_id")
  String transactionId;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  Instant createdAt;

  RateFee fee;

  public enum Type {
    QUOTE_CREATED("quote_created");

    @JsonValue public final String type;

    Type(String type) {
      this.type = type;
    }
  }

  public QuoteEvent() {}
}
