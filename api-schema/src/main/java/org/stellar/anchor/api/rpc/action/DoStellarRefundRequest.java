package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DoStellarRefundRequest extends RpcActionParamsRequest {

  @NotNull private Refund refund;

  @Data
  @Builder
  public static class Refund {

    @NotNull private AmountRequest amount;

    @SerializedName("amount_fee")
    @NotNull
    private AmountRequest amountFee;
  }
}