package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.sep.operation.ReceiveInfo;
import org.stellar.anchor.api.sep.operation.Sep38Info;

public interface AssetInfo {
  String NATIVE_ASSET_CODE = "native";

  /**
   * Returns the asset identification name following the structure of <scheme:identifier>
   * The currently accepted scheme values are:
   * - stellar: Used for Stellar assets. The identifier follows the SEP-11 asset format <Code:IssuerAccountID>.
   * - iso4217: Used for fiat currencies. The identifier follows the ISO 4217 three-character currency code.
   * For example:
   * - Stellar USDC would be identified as: stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN
   * - Fiat USD would be identified as: iso4217:USD
   *
   * @return A string representing the asset identification name, formatted as <scheme>:<identifier>.
   */
  String getId();

  String getCode();

  String getIssuer();

  Integer getSignificantDecimals();

  ReceiveInfo getSep31();

  Sep38Info getSep38();

  @Data
  class DepositWithdrawInfo {
    Boolean enabled = false;
    DepositWithdrawOperation deposit;
    DepositWithdrawOperation withdraw;
  }

  @Data
  class DepositWithdrawOperation {
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
  class Field {
    String description;
    List<String> choices;
    boolean optional;
  }

  enum Schema {
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
}
