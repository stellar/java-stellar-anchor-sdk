package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;

@Data
public class Sep31Info {
  Boolean enabled = false;

  ReceiveOperation receive;

  @SerializedName("quotes_supported")
  boolean quotesSupported;

  @SerializedName("quotes_required")
  boolean quotesRequired;

  Fields fields;

  @Data
  public static class ReceiveOperation {
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
  public static class Fields {
    Map<String, AssetInfo.Field> transaction;
  }
}
