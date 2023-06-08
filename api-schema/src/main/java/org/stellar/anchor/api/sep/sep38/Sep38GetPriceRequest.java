package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the GET /price endpoint of SEP-38.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0038.md#fields">Refer
 *     to SEP-38</a>
 */
@Data
@Builder
public class Sep38GetPriceRequest {
  Sep38Context context;

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
