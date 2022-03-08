package org.stellar.platform.apis.callbacks.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
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

  @SerializedName("sell_delivery_method")
  String sellDeliveryMethod;

  @SerializedName("buy_delivery_method")
  String buyDeliveryMethod;

  @SerializedName("client_domain")
  String clientDomain;

  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
