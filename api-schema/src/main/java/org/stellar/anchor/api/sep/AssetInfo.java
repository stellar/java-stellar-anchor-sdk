package org.stellar.anchor.api.sep;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.*;
import org.stellar.anchor.api.sep.operation.Sep31Info;
import org.stellar.anchor.api.sep.operation.Sep38Info;

@SuppressWarnings("unused")
@Data
public class AssetInfo {
  public static String NATIVE_ASSET_CODE = "native";

  Schema schema;

  String code;

  String issuer;

  @SerializedName("distribution_account")
  String distributionAccount;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  DepositWithdrawInfo sep6;

  DepositWithdrawInfo sep24;

  Sep31Info sep31;

  Sep38Info sep38;

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

  public enum Schema {
    @SerializedName("stellar")
    STELLAR("stellar"),

    @SerializedName("iso4217")
    ISO_4217("iso4217");

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
  public static class DepositWithdrawInfo {
    Boolean enabled = false;
    DepositWithdrawOperation deposit;
    DepositWithdrawOperation withdraw;
  }

  @Data
  public static class DepositWithdrawOperation {
    Boolean enabled = false;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;

    List<String> methods;
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
