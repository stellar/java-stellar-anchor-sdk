package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.*;

/** The response to the GET /info endpoint of SEP-24. */
@Getter
@Setter
@Builder
public class InfoResponse {
  Map<String, DepositInfo> deposit;
  Map<String, WithdrawInfo> withdraw;
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

  @Data
  @Builder
  public static class DepositInfo {
    Long minAmount;
    Long maxAmount;
  }

  @Data
  @Builder
  public static class WithdrawInfo {
    Long minAmount;
    Long maxAmount;
  }
}
