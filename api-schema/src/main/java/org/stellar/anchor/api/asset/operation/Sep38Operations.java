package org.stellar.anchor.api.asset.operation;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Sep38Operations extends AssetOperations {
  @SerializedName("exchangeable_assets")
  private List<String> exchangeableAssets;

  @SerializedName("country_codes")
  private List<String> countryCodes;

  @SerializedName("sell_delivery_methods")
  private List<DeliveryMethod> sellDeliveryMethods;

  @SerializedName("buy_delivery_methods")
  private List<DeliveryMethod> buyDeliveryMethods;

  private Integer decimals;

  @Data
  public static class DeliveryMethod {
    private String name;

    private String description;
  }
}
