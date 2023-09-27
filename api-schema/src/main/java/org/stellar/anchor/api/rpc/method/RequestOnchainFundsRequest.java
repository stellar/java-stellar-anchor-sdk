package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestOnchainFundsRequest extends RpcMethodParamsRequest {

  @SerializedName("amount_in")
  private AmountAssetRequest amountIn;

  @SerializedName("amount_out")
  private AmountAssetRequest amountOut;

  @SerializedName("amount_fee")
  private AmountAssetRequest amountFee;

  @SerializedName("amount_expected")
  private AmountRequest amountExpected;

  @SerializedName("destination_account")
  private String destinationAccount;

  @SerializedName("memo_type")
  private String memoType;

  private String memo;
}
