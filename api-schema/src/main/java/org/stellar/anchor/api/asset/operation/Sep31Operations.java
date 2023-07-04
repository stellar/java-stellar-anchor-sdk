package org.stellar.anchor.api.asset.operation;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Sep31Operations extends AssetOperations {

  @SerializedName("quotes_supported")
  private boolean quotesSupported;

  @SerializedName("quotes_required")
  private boolean quotesRequired;

  private Sep12Operation sep12;

  @Data
  public static class SendOperation {
    private boolean enabled;

    @SerializedName("fee_fixed")
    private Integer feeFixed;

    @SerializedName("fee_percent")
    private Integer feePercent;

    @SerializedName("min_amount")
    private Long minAmount;

    @SerializedName("max_amount")
    private Long maxAmount;

    private Fields fields;
  }

  @Data
  @Builder
  public static class Fields {
    private Map<String, Field> transaction;
  }

  private SendOperation send;

  @Data
  @AllArgsConstructor
  public static class Field {
    private String description;
    private List<String> choices;
    private boolean optional;
  }
}
