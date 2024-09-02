package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.sep.operation.Sep31Info;
import org.stellar.anchor.api.sep.operation.Sep38Info;

@Data
public class FiatAssetInfo implements AssetInfo {
  String id;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  Sep31Info sep31;
  FiatSep38Info sep38;

  @Override
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return null;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class FiatSep38Info extends Sep38Info {
    @SerializedName("sell_delivery_methods")
    List<DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    List<DeliveryMethod> buyDeliveryMethods;
  }
}
