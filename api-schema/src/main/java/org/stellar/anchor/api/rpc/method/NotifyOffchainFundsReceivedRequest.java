package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.RateFee;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsReceivedRequest extends RpcMethodParamsRequest {

  @SerializedName("funds_received_at")
  private Instant fundsReceivedAt;

  @SerializedName("external_transaction_id")
  private String externalTransactionId;

  @SerializedName("amount_in")
  private AmountRequest amountIn;

  @SerializedName("amount_out")
  private AmountRequest amountOut;

  @SerializedName("amount_fee")
  @Deprecated
  private AmountRequest amountFee;

  @SerializedName("fee_details")
  private RateFee feeDetails;
}
