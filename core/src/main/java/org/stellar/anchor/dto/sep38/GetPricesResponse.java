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

  public void addAsset(@NonNull String assetName, @NonNull String price) {
    int decimals = assetName.startsWith("iso4217") ? 2 : 7;
    buyAssets.add(Asset.builder().asset(assetName).price(price).decimals(decimals).build());
  }

  @Data
  @Builder
  public static class Asset {
    String asset;
    String price;
    Integer decimals;
  }
}
