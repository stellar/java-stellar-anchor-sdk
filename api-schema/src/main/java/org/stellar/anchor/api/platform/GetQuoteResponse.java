package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.shared.Amount;

@EqualsAndHashCode(callSuper = false)
@Data
public class GetQuoteResponse {
  String id;

  @SerializedName("sell_amount")
  Amount sellAmount;

  @SerializedName("buy_amount")
  Amount buyAmount;

  @SerializedName("expires_at")
  Instant expiresAt;

  @SerializedName("created_at")
  Instant createdAt;

  String price;
  String stellarAccount;
  String memo;
  String memoType;

  @SerializedName("transaction_id")
  String transactionId;
}
