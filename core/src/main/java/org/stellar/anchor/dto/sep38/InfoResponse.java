package org.stellar.anchor.dto.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.stellar.anchor.asset.AssetResponse;

@Data
public class InfoResponse {
  private List<AssetInfo> assets = new ArrayList<>();

  public InfoResponse(List<AssetResponse> assetResponseList) {
    assetResponseList.forEach(
        assetResponse -> {
          AssetInfo newAssetInfo = new AssetInfo();
          String assetName = assetResponse.getSchema().toString() + ":" + assetResponse.getCode();
          if (!Objects.toString(assetResponse.getIssuer(), "").isEmpty()) {
            assetName += ":" + assetResponse.getIssuer();
          }
          newAssetInfo.setAsset(assetName);

          AssetResponse.Sep38Operation sep38Info = assetResponse.getSep38();
          newAssetInfo.setCountryCodes(sep38Info.getCountryCodes());
          newAssetInfo.setSellDeliveryMethods(sep38Info.getSellDeliveryMethods());
          newAssetInfo.setBuyDeliveryMethods(sep38Info.getBuyDeliveryMethods());
          newAssetInfo.setExchangeableAssetNames(sep38Info.getExchangeableAssets());

          assets.add(newAssetInfo);
        });
  }

  @Data
  public static class AssetInfo {
    private String asset;

    @SerializedName("country_codes")
    private List<String> countryCodes;

    @SerializedName("sell_delivery_methods")
    private List<AssetResponse.Sep38Operation.DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    private List<AssetResponse.Sep38Operation.DeliveryMethod> buyDeliveryMethods;

    private transient List<String> exchangeableAssetNames;
  }
}
