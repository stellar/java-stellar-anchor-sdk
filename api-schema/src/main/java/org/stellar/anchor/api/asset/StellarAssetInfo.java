package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo.DepositWithdrawInfo;
import org.stellar.anchor.api.sep.operation.Sep31Info;
import org.stellar.anchor.api.sep.operation.Sep38Info;
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse;
import org.stellar.anchor.api.sep.sep38.InfoResponse;

@Data
public class StellarAssetInfo implements AssetInfo {
  String id;

  @SerializedName("distribution_account")
  String distributionAccount;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  DepositWithdrawInfo sep6;
  DepositWithdrawInfo sep24;
  Sep31Info sep31;
  Sep38Info sep38;

  @Override
  public InfoResponse.Asset toSEP38InfoResponseAsset() {
    return null;
  }

  @Override
  public Sep31InfoResponse.AssetResponse toSEP31InfoResponseAsset() {
    return null;
  }
}
