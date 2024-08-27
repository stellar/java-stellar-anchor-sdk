package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
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
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return getCode().equals(NATIVE_ASSET_CODE) ? null : getId().split(":")[2];
  }

  @Override
  public Sep31InfoResponse.AssetResponse toSEP31InfoResponseAsset() {
    if (getSep31() != null && getSep31().getEnabled()) {
      Sep31InfoResponse.AssetResponse assetResponse = new Sep31InfoResponse.AssetResponse();
      assetResponse.setQuotesSupported(getSep31().isQuotesSupported());
      assetResponse.setQuotesRequired(getSep31().isQuotesRequired());
      assetResponse.setFeeFixed(getSep31().getReceive().getFeeFixed());
      assetResponse.setFeePercent(getSep31().getReceive().getFeePercent());
      assetResponse.setMinAmount(getSep31().getReceive().getMinAmount());
      assetResponse.setMaxAmount(getSep31().getReceive().getMaxAmount());
      assetResponse.setFields(getSep31().getFields());
      return assetResponse;
    }
    return null;
  }

  @Override
  public InfoResponse.Asset toSEP38InfoResponseAsset() {
    if (getSep38().getEnabled()) {
      InfoResponse.Asset newAsset = new InfoResponse.Asset();
      newAsset.setAsset(getId());

      Sep38Info sep38Info = getSep38();
      newAsset.setCountryCodes(sep38Info.getCountryCodes());
      newAsset.setExchangeableAssetNames(sep38Info.getExchangeableAssets());

      newAsset.setDecimals(7);
      return newAsset;
    }
    return null;
  }

  /**
   * Determines if deposit or withdraw service is enabled in SEP-6 or SEP-24.
   *
   * @param info The DepositWithdrawInfo containing the service details.
   * @param service The operation to check, either "deposit" or "withdraw" (case-insensitive).
   * @return true if the specified operation is enabled; false otherwise.
   */
  public boolean getIsServiceEnabled(DepositWithdrawInfo info, String service) {
    if (info == null || !info.getEnabled()) {
      return false;
    }

    DepositWithdrawOperation operation;
    switch (service.toLowerCase()) {
      case "deposit":
        operation = info.getDeposit();
        break;
      case "withdraw":
        operation = info.getWithdraw();
        break;
      default:
        return false;
    }
    return operation != null && operation.getEnabled();
  }
}
