package org.stellar.anchor.api.sep.sep38;

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
      @NonNull String buyAssetName,
      Integer buyAssetDecimals,
      @NonNull String price,
      List<PriceDetail> priceDetails) {
    buyAssets.add(
        Asset.builder()
            .asset(buyAssetName)
            .price(price)
            .decimals(buyAssetDecimals)
            .priceDetails(priceDetails)
            .build());
  }

  @Data
  @Builder
  public static class Asset {
    String asset;
    String price;
    Integer decimals;

    @SerializedName("price_details")
    List<PriceDetail> priceDetails;
  }
}
