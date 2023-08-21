package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsReceivedRequest extends RpcMethodParamsRequest {

  @NotBlank
  @SerializedName("stellar_transaction_id")
  private String stellarTransactionId;

  @SerializedName("amount_in")
  private AmountRequest amountIn;

  @SerializedName("amount_out")
  private AmountRequest amountOut;

  @SerializedName("amount_fee")
  private AmountRequest amountFee;
}
