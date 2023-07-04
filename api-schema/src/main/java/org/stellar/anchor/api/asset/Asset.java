package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.api.asset.operation.Sep24Operations;
import org.stellar.anchor.api.asset.operation.Sep31Operations;
import org.stellar.anchor.api.asset.operation.Sep38Operations;

@Data
public class Asset {
  public static String NATIVE_ASSET_CODE = "native";

  private Schema schema;
  private String code;
  private String issuer;

  @SerializedName("distribution_account")
  private String distributionAccount;

  @SerializedName("significant_decimals")
  private Integer significantDecimals;

  private Operations operations;

  public enum Schema {
    @SerializedName("stellar")
    STELLAR,

    @SerializedName("iso4217")
    ISO_4217
  }

  public String getAssetName() {
    if (issuer != null) {
      return schema + ":" + code + ":" + issuer;
    }
    return schema + ":" + code;
  }

  @Data
  public static class Operations {
    private Sep24Operations sep24;
    private Sep31Operations sep31;
    private Sep38Operations sep38;
  }
}
