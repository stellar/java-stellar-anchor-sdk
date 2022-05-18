package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPriceResponse {
  String price;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_amount")
  String buyAmount;

  @SerializedName("price_details")
  List<PriceDetail> priceDetails;
}
