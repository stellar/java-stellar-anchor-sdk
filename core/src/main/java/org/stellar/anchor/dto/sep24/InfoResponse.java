package org.stellar.anchor.dto.sep24;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.asset.AssetResponse;

@Data
public class InfoResponse {
  Map<String, AssetResponse.AssetOperation> deposit = new HashMap<>();
  Map<String, AssetResponse.AssetOperation> withdraw = new HashMap<>();
  FeeResponse fee = new FeeResponse();

  @SerializedName("feature_flags")
  FeatureFlagResponse featureFlags = new FeatureFlagResponse(true, true);

  @SuppressWarnings("unused")
  @Data
  public static class FeeResponse {
    final Boolean enabled;

    public FeeResponse() {
      this.enabled = true;
    }
  }

  @Data
  public static class FeatureFlagResponse {
    @SerializedName("account_creation")
    Boolean accountCreation;

    @SerializedName("claimable_balances")
    Boolean claimableBalances;

    public FeatureFlagResponse() {
      this.accountCreation = true;
      this.claimableBalances = true;
    }

    public FeatureFlagResponse(boolean accountCreation, boolean claimableBalances) {
      this.accountCreation = accountCreation;
      this.claimableBalances = claimableBalances;
    }
  }
}
