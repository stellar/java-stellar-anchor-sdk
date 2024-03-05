package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.RateFee;

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

  @SerializedName("amount_fee")
  @Deprecated
  private AmountAssetRequest amountFee;

  @SerializedName("fee_details")
  private RateFee feeDetails;

  @SerializedName("amount_expected")
  private AmountRequest amountExpected;
}
