package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsRequest extends RpcActionParamsRequest {

  @SerializedName("amount_in")
  private AmountAssetRequest amountIn;

  @SerializedName("amount_out")
  private AmountAssetRequest amountOut;

  @SerializedName("amount_fee")
  private AmountAssetRequest amountFee;

  @SerializedName("amount_expected")
  private AmountRequest amountExpected;
}
