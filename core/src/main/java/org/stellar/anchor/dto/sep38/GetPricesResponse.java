package org.stellar.anchor.dto.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
public class GetPricesResponse {
  @SerializedName("buy_assets")
  List<Asset> buyAssets = new ArrayList<>();

  public void addAsset(
      @NonNull String buyAssetName, Integer buyAssetDecimals, @NonNull String price) {
    int decimals = 7;
    if (!buyAssetName.startsWith("stellar") && buyAssetDecimals != null) {
      decimals = buyAssetDecimals;
    }

    buyAssets.add(Asset.builder().asset(buyAssetName).price(price).decimals(decimals).build());
  }

  public void addAsset(@NonNull String buyAssetName, @NonNull String price) {
    addAsset(buyAssetName, null, price);
  }

  @Data
  @Builder
  public static class Asset {
    String asset;
    String price;
    Integer decimals;
  }
}
