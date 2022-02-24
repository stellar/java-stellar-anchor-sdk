package org.stellar.platform.callbacks.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GetFeeRequest {
  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("client_domain")
  String clientDomain;

  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
