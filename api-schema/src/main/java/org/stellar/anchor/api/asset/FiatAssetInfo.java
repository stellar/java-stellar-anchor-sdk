package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.sep.operation.Sep31Info;
import org.stellar.anchor.api.sep.operation.Sep38Info;
import org.stellar.anchor.api.sep.sep31.Sep31InfoResponse;
import org.stellar.anchor.api.sep.sep38.InfoResponse;

@Data
public class FiatAssetInfo implements AssetInfo {
  String id;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  Sep31Info sep31;
  FiatSep38Info sep38;

  @Override
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return null;
  }

  @Override
  public Sep31InfoResponse.AssetResponse toSEP31InfoResponseAsset() {
    if (getSep31() != null && getSep31().getEnabled()) {
      Sep31InfoResponse.AssetResponse assetResponse = new Sep31InfoResponse.AssetResponse();
      assetResponse.setQuotesSupported(getSep31().isQuotesSupported());
      assetResponse.setQuotesRequired(getSep31().isQuotesSupported());
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
      InfoResponse.Asset assetResponse = new InfoResponse.Asset();
      assetResponse.setAsset(getId());

      FiatSep38Info sep38Info = getSep38();
      assetResponse.setCountryCodes(sep38Info.getCountryCodes());
      assetResponse.setSellDeliveryMethods(sep38Info.getSellDeliveryMethods());
      assetResponse.setBuyDeliveryMethods(sep38Info.getBuyDeliveryMethods());
      assetResponse.setExchangeableAssetNames(sep38Info.getExchangeableAssets());

      int decimals = sep38Info.getDecimals() != null ? sep38Info.getDecimals() : 7;
      assetResponse.setDecimals(decimals);

      return assetResponse;
    }
    return null;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class FiatSep38Info extends Sep38Info {
    @SerializedName("sell_delivery_methods")
    List<DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    List<DeliveryMethod> buyDeliveryMethods;
  }
}
