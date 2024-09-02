package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.asset.AssetInfo;
import org.stellar.anchor.api.asset.FiatAssetInfo;
import org.stellar.anchor.api.asset.Sep38Info;

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
      if (assetInfo.getSep38() == null
          || assetInfo.getSep38().getEnabled() == null
          || !assetInfo.getSep38().getEnabled()) continue;
      Asset newAsset = new Asset();
      Sep38Info sep38Info = assetInfo.getSep38();

      newAsset.setAsset(assetInfo.getId());
      newAsset.setCountryCodes(sep38Info.getCountryCodes());
      newAsset.setExchangeableAssetNames(sep38Info.getExchangeableAssets());

      int decimals = 7;
      if (assetInfo instanceof FiatAssetInfo fiatAssetInfo) {
        newAsset.setSellDeliveryMethods(fiatAssetInfo.getSep38().getSellDeliveryMethods());
        newAsset.setBuyDeliveryMethods(fiatAssetInfo.getSep38().getBuyDeliveryMethods());
        decimals = sep38Info.getDecimals() != null ? sep38Info.getDecimals() : 7;
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
    private List<Sep38Info.DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    private List<Sep38Info.DeliveryMethod> buyDeliveryMethods;

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
        List<Sep38Info.DeliveryMethod> deliveryMethods, String method) {
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

      Sep38Info.DeliveryMethod foundMethod =
          deliveryMethods.stream()
              .filter(dMethod -> dMethod.getName().equals(method))
              .findFirst()
              .orElse(null);
      return foundMethod != null;
    }
  }
}
