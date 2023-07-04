package org.stellar.anchor.api.asset.operation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Sep24Operations extends AssetOperations {

  @Data
  public static class DepositOperation {
    private boolean enabled;

    @SerializedName("min_amount")
    private Long minAmount;

    @SerializedName("max_amount")
    private Long maxAmount;
  }

  private DepositOperation deposit;

  @Data
  public static class WithdrawOperation {
    private boolean enabled;

    @SerializedName("min_amount")
    private Long minAmount;

    @SerializedName("max_amount")
    private Long maxAmount;
  }

  private WithdrawOperation withdraw;
}
