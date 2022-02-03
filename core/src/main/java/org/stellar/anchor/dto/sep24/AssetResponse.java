package org.stellar.anchor.dto.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class AssetResponse {
  String code;
  String issuer;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  AssetOperation deposit;
  AssetOperation withdraw;
  SendOperation send;

  @SerializedName("sep24_enabled")
  Boolean sep24Enabled;

  @SerializedName("sep6_enabled")
  Boolean sep6Enabled;

  @SerializedName("sep31_enabled")
  Boolean sep31Enabled;

  @Data
  public static class AssetOperation {
    Boolean enabled;

    @SerializedName("fee_fixed")
    int feeFixed;

    @SerializedName("fee_percent")
    Integer feePercent;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;

    @SerializedName("fee_minimum")
    Long feeMinimum;
  }

  @Data
  public static class SendOperation {
    @SerializedName("fee_fixed")
    Integer feeFixed;

    @SerializedName("fee_percent")
    Integer feePercent;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;
  }
}
