package org.stellar.platform.apis.shared;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Quote {
  String id;

  @SerializedName("sell_amount")
  Amount sellAmount;

  @SerializedName("buy_amount")
  Amount buyAmount;

  @SerializedName("expires_at")
  LocalDateTime expiresAt;

  @SerializedName("created_at")
  LocalDateTime createdAt;

  String price;
  String stellarAccount;
  String memo;
  String memoType;

  @SerializedName("transaction_id")
  String transactionId;
}
