package org.stellar.anchor.api.sep.sep24;

import static org.stellar.anchor.api.sep.AssetInfo.AssetOperation;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.*;

/** The response to the GET /info endpoint of SEP-24. */
@Getter
@Setter
@Builder
public class InfoResponse {
  Map<String, OperationResponse> deposit;
  Map<String, OperationResponse> withdraw;
  FeeResponse fee;
  FeatureFlagResponse features;

  @Data
  @Builder
  public static class OperationResponse {
    Boolean enabled;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;

    public static OperationResponse fromAssetOperation(AssetOperation operation) {
      return OperationResponse.builder()
          .enabled(operation.getEnabled())
          .minAmount(operation.getMinAmount())
          .maxAmount(operation.getMaxAmount())
          .build();
    }
  }

  @SuppressWarnings("unused")
  @Getter
  @Setter
  @AllArgsConstructor
  public static class FeeResponse {
    Boolean enabled;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  public static class FeatureFlagResponse {
    @SerializedName("account_creation")
    Boolean accountCreation;

    @SerializedName("claimable_balances")
    Boolean claimableBalances;
  }
}
