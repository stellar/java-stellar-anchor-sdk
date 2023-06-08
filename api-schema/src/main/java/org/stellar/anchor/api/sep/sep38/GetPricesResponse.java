package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * The response body of the GET /prices endpoint of SEP-38.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0038.md#fields">Refer
 *     to SEP-38</a>
 */
@Data
public class GetPricesResponse {
  @SerializedName("buy_assets")
  List<Asset> buyAssets = new ArrayList<>();

  public void addAsset(
      @NonNull String buyAssetName, Integer buyAssetDecimals, @NonNull String price) {
    buyAssets.add(
        Asset.builder().asset(buyAssetName).price(price).decimals(buyAssetDecimals).build());
  }

  @Data
  @Builder
  public static class Asset {
    String asset;
    String price;
    Integer decimals;
  }
}
