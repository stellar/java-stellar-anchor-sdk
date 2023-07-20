package org.stellar.anchor.api.sep;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.*;
import org.stellar.anchor.api.sep.operation.Sep31Operation;
import org.stellar.anchor.api.sep.operation.Sep38Operation;

@SuppressWarnings("unused")
@Data
public class AssetInfo {
  public static String NATIVE_ASSET_CODE = "native";

  String code;
  String issuer;

  public String getAssetName() {
    if (issuer != null) {
      return schema + ":" + code + ":" + issuer;
    }
    return schema + ":" + code;
  }

  @SerializedName("distribution_account")
  String distributionAccount;

  Schema schema;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  DepositOperation deposit;
  WithdrawOperation withdraw;
  SendOperation send;
  Sep31Operation sep31;
  Sep38Operation sep38;

  @SerializedName("sep6_enabled")
  Boolean sep6Enabled = false;

  @SerializedName("sep24_enabled")
  Boolean sep24Enabled = false;

  @SerializedName("sep31_enabled")
  Boolean sep31Enabled = false;

  @SerializedName("sep38_enabled")
  Boolean sep38Enabled = false;

  public enum Schema {
    @SerializedName("stellar")
    stellar("stellar"),

    @SerializedName("iso4217")
    iso4217("iso4217");

    private final String name;

    Schema(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  @Data
  public static class AssetOperation {
    Boolean enabled;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class DepositOperation extends AssetOperation {
    List<String> methods;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class WithdrawOperation extends AssetOperation {
    List<String> methods;
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

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Field {
    String description;
    List<String> choices;
    boolean optional;
  }
}
