package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.callback.GetRateRequest;

@Data
@Builder
public class Sep38GetPriceRequest {
  GetRateRequest.Context context;

  @SerializedName("sell_asset")
  String sellAssetName;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("sell_delivery_method")
  String sellDeliveryMethod;

  @SerializedName("buy_asset")
  String buyAssetName;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("buy_delivery_method")
  String buyDeliveryMethod;

  @SerializedName("country_code")
  String countryCode;
}
