package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Data;
import org.stellar.anchor.api.asset.Asset;
import org.stellar.anchor.api.asset.operation.AssetOperations;
import org.stellar.anchor.api.asset.operation.Sep38Operations;
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
  private List<AssetResponse> assets = new ArrayList<>();

  public InfoResponse(List<Asset> assetInfoList) {
    for (Asset assetInfo : assetInfoList) {
      Optional<Sep38Operations> operation =
          Optional.ofNullable(assetInfo.getOperations()).map(Asset.Operations::getSep38);
      if (!operation.map(AssetOperations::isEnabled).orElse(false)) continue;
      AssetResponse newAsset = new AssetResponse();
      String assetName = assetInfo.getSchema().toString() + ":" + assetInfo.getCode();
      if (!Objects.toString(assetInfo.getIssuer(), "").isEmpty()) {
        assetName += ":" + assetInfo.getIssuer();
      }
      newAsset.setAsset(assetName);

      newAsset.setCountryCodes(operation.get().getCountryCodes());
      newAsset.setSellDeliveryMethods(operation.get().getSellDeliveryMethods());
      newAsset.setBuyDeliveryMethods(operation.get().getBuyDeliveryMethods());
      newAsset.setExchangeableAssetNames(operation.get().getExchangeableAssets());

      int decimals = 7;
      if (!assetName.startsWith("stellar") && operation.get().getDecimals() != null) {
        decimals = operation.get().getDecimals();
      }
      newAsset.setDecimals(decimals);

      assets.add(newAsset);
    }
  }

  @Data
  public static class AssetResponse {
    private String asset;

    @SerializedName("country_codes")
    private List<String> countryCodes;

    @SerializedName("sell_delivery_methods")
    private List<Sep38Operations.DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    private List<Sep38Operations.DeliveryMethod> buyDeliveryMethods;

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
