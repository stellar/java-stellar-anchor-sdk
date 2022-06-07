package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.sep.sep38.Sep38Context;

@Data
@Builder
public class GetRateRequest {
  Type type;

  Sep38Context context;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("sell_delivery_method")
  String sellDeliveryMethod;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("buy_delivery_method")
  String buyDeliveryMethod;

  @SerializedName("country_code")
  String countryCode;

  @SerializedName("expire_after")
  String expireAfter;

  @SerializedName("client_id")
  String clientId;

  String id;

  public enum Type {
    @SerializedName("indicative_prices")
    INDICATIVE_PRICES("indicative_prices"),

    @SerializedName("indicative_price")
    INDICATIVE_PRICE("indicative_price"),

    @SerializedName("firm")
    FIRM("firm");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
