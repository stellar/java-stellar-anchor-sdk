package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.api.sep.operation.ReceiveInfo;
import org.stellar.anchor.api.sep.operation.Sep38Info;

@Data
public class StellarAssetInfo implements AssetInfo {
  String id;

  @SerializedName("distribution_account")
  String distributionAccount;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  DepositWithdrawInfo sep6;
  DepositWithdrawInfo sep24;
  ReceiveInfo sep31;
  Sep38Info sep38;

  @Override
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return getCode().equals(NATIVE_ASSET_CODE) ? null : getId().split(":")[2];
  }

  public boolean isDepositEnabled(DepositWithdrawInfo info) {
    if (info == null || !info.getEnabled()) {
      return false;
    }
    DepositWithdrawOperation operation = info.getDeposit();
    return operation != null && operation.getEnabled();
  }

  public boolean isWithdrawEnabled(DepositWithdrawInfo info) {
    if (info == null || !info.getEnabled()) {
      return false;
    }
    DepositWithdrawOperation operation = info.getWithdraw();
    return operation != null && operation.getEnabled();
  }
}
