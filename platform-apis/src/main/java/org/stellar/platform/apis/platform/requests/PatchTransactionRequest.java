package org.stellar.platform.apis.platform.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.stellar.platform.apis.shared.Amount;
import org.stellar.platform.apis.shared.Refunds;

import java.time.Instant;

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
    Refunds refunds;

    @SerializedName("external_transaction_id")
    String externalTransactionId;
}
