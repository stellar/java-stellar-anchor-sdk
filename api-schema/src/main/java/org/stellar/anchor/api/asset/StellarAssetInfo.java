package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class StellarAssetInfo implements AssetInfo {
  String id;

  @SerializedName("distribution_account")
  String distributionAccount;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  Sep6Info sep6;
  Sep24Info sep24;
  Sep31Info sep31;
  Sep38Info sep38;

  @Override
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return getCode().equals(NATIVE_ASSET_CODE) ? null : getId().split(":")[2];
  }
}
