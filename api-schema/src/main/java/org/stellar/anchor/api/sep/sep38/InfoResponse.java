package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

/**
 * The response body of the GET /info endpoint of SEP-38.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0038.md#fields">Refer
 *     to SEP-38</a>
 */
@Data
public class InfoResponse {
  private List<Asset> assets = new ArrayList<>();

  public InfoResponse(List<AssetInfo> assetInfoList) {
    for (AssetInfo assetInfo : assetInfoList) {
      if (!assetInfo.getSep38Enabled()) continue;
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

      int decimals = 7;
      if (!assetName.startsWith("stellar") && sep38Info.getDecimals() != null) {
        decimals = sep38Info.getDecimals();
      }
      newAsset.setDecimals(decimals);

      assets.add(newAsset);
    }
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

    private transient Integer decimals;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean supportsSellDeliveryMethod(String deliveryMethod) {
      return supportsDeliveryMethod(sellDeliveryMethods, deliveryMethod);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean supportsBuyDeliveryMethod(String deliveryMethod) {
      return supportsDeliveryMethod(buyDeliveryMethods, deliveryMethod);
    }

    private boolean supportsDeliveryMethod(
        List<AssetInfo.Sep38Operation.DeliveryMethod> deliveryMethods, String method) {
      boolean noneIsAvailable = deliveryMethods == null || deliveryMethods.size() == 0;
      boolean noneIsProvided = method == null || method.equals("");
      if (noneIsAvailable && noneIsProvided) {
        return true;
      }

      if (noneIsAvailable) {
        return false;
      }

      if (noneIsProvided) {
        return true;
      }

      AssetInfo.Sep38Operation.DeliveryMethod foundMethod =
          deliveryMethods.stream()
              .filter(dMethod -> dMethod.getName().equals(method))
              .findFirst()
              .orElse(null);
      return foundMethod != null;
    }
  }
}
