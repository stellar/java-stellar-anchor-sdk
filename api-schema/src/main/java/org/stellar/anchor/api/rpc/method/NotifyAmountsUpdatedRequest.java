package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.FeeDetails;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyAmountsUpdatedRequest extends RpcMethodParamsRequest {

  @SerializedName("amount_out")
  @NotNull
  private AmountRequest amountOut;

  @SerializedName("fee_details")
  private FeeDetails feeDetails;
}
