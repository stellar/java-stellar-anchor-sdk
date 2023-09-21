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

  /**
   * Returns the SEP-11 asset name, which is the asset code and issuer separated by a colon. If the
   * asset is the native asset, the name is "native".
   *
   * @return The SEP-11 asset name.
   */
  public String getSep11AssetName() {
    return makeSep11AssetName(code, issuer);
  }

  /**
   * Returns the SEP-38 asset name, which is the SEP-11 asset name prefixed with the schema.
   *
   * @return The SEP-38 asset name.
   */
  public String getSep38AssetName() {
    return schema + ":" + makeSep11AssetName(code, issuer);
  }

  /**
   * Returns the SEP-11 asset name for the given asset code and issuer.
   *
   * @param assetCode The asset code.
   * @param assetIssuer The asset issuer.
   * @return The SEP-11 asset name.
   */
  public static String makeSep11AssetName(String assetCode, String assetIssuer) {
    if (AssetInfo.NATIVE_ASSET_CODE.equals(assetCode)) {
      return AssetInfo.NATIVE_ASSET_CODE;
    } else if (assetIssuer != null) {
      return assetCode + ":" + assetIssuer;
    } else {
      return assetCode;
    }
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
