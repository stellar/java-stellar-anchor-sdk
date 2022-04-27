package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetRateRequest {
  String type;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("country_code")
  String countryCode;

  @SerializedName("sell_delivery_method")
  String sellDeliveryMethod;

  @SerializedName("buy_delivery_method")
  String buyDeliveryMethod;

  @SerializedName("expires_after")
  Instant expiresAfter;

  @SerializedName("client_id")
  String clientId;

  String id;
}
