package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsReceivedRequest extends RpcActionParamsRequest {

  @SerializedName("funds_received_at")
  private Instant fundsReceivedAt;

  @SerializedName("external_transaction_id")
  private String externalTransactionId;

  @SerializedName("amount_in")
  private String amountIn;

  @SerializedName("amount_out")
  private String amountOut;

  @SerializedName("amount_fee")
  private String amountFee;
}
