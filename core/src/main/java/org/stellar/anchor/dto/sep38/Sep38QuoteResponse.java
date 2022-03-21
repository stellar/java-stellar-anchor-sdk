package org.stellar.anchor.dto.sep38;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sep38QuoteResponse {
  String id;

  @SerializedName("expires_at")
  LocalDateTime expiresAt;

  String price;

  @SerializedName("sell_asset")
  String sellAsset;

  @SerializedName("sell_amount")
  String sellAmount;

  @SerializedName("buy_asset")
  String buyAsset;

  @SerializedName("buy_amount")
  String buyAmount;
}
