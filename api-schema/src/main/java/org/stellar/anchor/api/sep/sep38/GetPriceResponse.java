package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPriceResponse {
  String price;

  @SerializedName("total_price")
  String totalPrice;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_amount")
  String buyAmount;

  RateFee fee;
}
