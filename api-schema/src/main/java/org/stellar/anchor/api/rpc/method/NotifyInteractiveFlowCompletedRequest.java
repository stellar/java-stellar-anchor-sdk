package org.stellar.anchor.api.rpc.method;

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
public class NotifyInteractiveFlowCompletedRequest extends RpcMethodParamsRequest {

  @NotNull
  @SerializedName("amount_in")
  private AmountAssetRequest amountIn;

  @NotNull
  @SerializedName("amount_out")
  private AmountAssetRequest amountOut;

  @NotNull
  @SerializedName("amount_fee")
  private AmountAssetRequest amountFee;

  @SerializedName("amount_expected")
  private AmountRequest amountExpected;
}
