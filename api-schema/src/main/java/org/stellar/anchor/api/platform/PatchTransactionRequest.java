package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.Refund;

@Data
@Builder
public class PatchTransactionRequest {
  String id;
  String status;

  @SerializedName("amount_in")
  Amount amountIn;

  @SerializedName("amount_out")
  Amount amountOut;

  @SerializedName("amount_fee")
  Amount amountFee;

  @SerializedName("transfer_received_at")
  Instant transferReceivedAt;

  String message;
  Refund refunds;

  @SerializedName("external_transaction_id")
  String externalTransactionId;
}
