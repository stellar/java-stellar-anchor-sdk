package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.sep.AssetInfo.Schema;
import org.stellar.anchor.api.sep.operation.Sep31Info;
import org.stellar.anchor.api.sep.operation.Sep38Info;
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse;
import org.stellar.anchor.api.sep.sep38.InfoResponse;

public class FiatAssetInfo implements AssetInfo {
  String code;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  Sep31Info sep31;
  FiatSep38Info sep38;

  @Override
  public Schema getSchema() {
    return Schema.ISO_4217;
  }

  @Override
  public String getAssetIdentificationName() {
    return getSchema() + ":" + code;
  }

  @Override
  public InfoResponse.Asset toSEP38InfoResponseAsset() {
    return null;
  }

  @Override
  public Sep31InfoResponse.AssetResponse toSEP31InfoResponseAsset() {
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
