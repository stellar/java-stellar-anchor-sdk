package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyInteractiveFlowCompletedRequest extends RpcActionParamsRequest {

  @SerializedName("amount_in")
  @NotNull
  private AmountRequest amountIn;

  @SerializedName("amount_out")
  @NotNull
  private AmountRequest amountOut;

  @SerializedName("amount_fee")
  @NotNull
  private AmountRequest amountFee;

  @SerializedName("amount_expected")
  private String amountExpected;
}
