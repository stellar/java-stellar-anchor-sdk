package org.stellar.anchor.api.sep.sep24;

import static org.stellar.anchor.api.sep.AssetInfo.AssetOperation;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** The response to the GET /info endpoint of SEP-24. */
@Getter
@Setter
@Builder
public class InfoResponse {
  Map<String, AssetOperation> deposit;
  Map<String, AssetOperation> withdraw;
  FeeResponse fee;
  FeatureFlagResponse features;

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
