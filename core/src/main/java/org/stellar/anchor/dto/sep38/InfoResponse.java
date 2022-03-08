package org.stellar.anchor.dto.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.stellar.anchor.asset.AssetInfo;

@Data
public class InfoResponse {
  private List<Asset> assets = new ArrayList<>();

  public InfoResponse(List<AssetInfo> assetInfoList) {
    assetInfoList.forEach(
        assetInfo -> {
          Asset newAsset = new Asset();
          String assetName = assetInfo.getSchema().toString() + ":" + assetInfo.getCode();
          if (!Objects.toString(assetInfo.getIssuer(), "").isEmpty()) {
            assetName += ":" + assetInfo.getIssuer();
          }
          newAsset.setAsset(assetName);

          AssetInfo.Sep38Operation sep38Info = assetInfo.getSep38();
          newAsset.setCountryCodes(sep38Info.getCountryCodes());
          newAsset.setSellDeliveryMethods(sep38Info.getSellDeliveryMethods());
          newAsset.setBuyDeliveryMethods(sep38Info.getBuyDeliveryMethods());
          newAsset.setExchangeableAssetNames(sep38Info.getExchangeableAssets());

          assets.add(newAsset);
        });
  }

  @Data
  public static class Asset {
    private String asset;

    @SerializedName("country_codes")
    private List<String> countryCodes;

    @SerializedName("sell_delivery_methods")
    private List<AssetInfo.Sep38Operation.DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    private List<AssetInfo.Sep38Operation.DeliveryMethod> buyDeliveryMethods;

    private transient List<String> exchangeableAssetNames;
  }
}
