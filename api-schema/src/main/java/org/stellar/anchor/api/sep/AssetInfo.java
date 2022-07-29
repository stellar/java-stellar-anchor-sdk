package org.stellar.anchor.api.sep;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class AssetInfo {
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

  @SerializedName("sep24_enabled")
  Boolean sep24Enabled = false;

  @SerializedName("sep31_enabled")
  Boolean sep31Enabled = false;

  @SerializedName("sep38_enabled")
  Boolean sep38Enabled = false;

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

  public static class DepositOperation extends AssetOperation {}

  public static class WithdrawOperation extends AssetOperation {}

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
  public static class Sep31Operation {
    @SerializedName("quotes_supported")
    boolean quotesSupported;

    @SerializedName("quotes_required")
    boolean quotesRequired;

    Sep12Operation sep12;
    Sep31TxnFieldSpecs fields;
  }

  @Data
  public static class Sep12Operation {
    Sep12Types sender;
    Sep12Types receiver;
  }

  @Data
  public static class Sep12Types {
    Map<String, Sep12Type> types;
  }

  @Data
  public static class Sep12Type {
    String description;
  }

  @Data
  public static class Sep31TxnFieldSpecs {
    Map<String, Sep31TxnFieldSpec> transaction;
  }

  @Data
  @AllArgsConstructor
  public static class Sep31TxnFieldSpec {
    String description;
    List<String> choices;
    boolean optional;

    public Sep31TxnFieldSpec() {}
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

    Integer decimals;

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
