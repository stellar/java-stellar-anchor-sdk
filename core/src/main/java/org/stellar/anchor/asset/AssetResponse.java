package org.stellar.anchor.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class AssetResponse {
  String code;
  String issuer;
  Schema schema;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  AssetOperation deposit;
  AssetOperation withdraw;
  SendOperation send;
  Sep38Operation sep38;

  @SerializedName("sep24_enabled")
  Boolean sep24Enabled;

  @SerializedName("sep6_enabled")
  Boolean sep6Enabled;

  @SerializedName("sep31_enabled")
  Boolean sep31Enabled;

  @SerializedName("sep38_enabled")
  Boolean sep38Enabled;

  public enum Schema {
    @SerializedName("stellar")
    STELLAR("stellar"),

    @SerializedName("iso4217")
    ISO4217("iso4217");

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

  @Data
  public static class Sep38Operation {
    @SerializedName("exchangeable_assets")
    List<String> exchangeableAssets;

    @SerializedName("country_codes")
    List<String> countryCodes;

    @SerializedName("sell_delivery_methods")
    List<DeliveryMethod> sellDeliveryMethods;

    @SerializedName("buy_delivery_methods")
    List<DeliveryMethod> buyDeliveryMethods;

    @Data
    public static class DeliveryMethod {
      String name;

      String description;

      public DeliveryMethod(String name, String description) {
        this.name = name;
        this.description = description;
      }
    }
  }
}
