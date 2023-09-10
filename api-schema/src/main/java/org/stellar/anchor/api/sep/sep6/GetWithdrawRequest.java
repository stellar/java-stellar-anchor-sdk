package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class GetWithdrawRequest {
  @SerializedName("asset_code")
  @NonNull
  String assetCode;

  @NonNull String type;

  @NonNull String amount;

  @SerializedName("country_code")
  String countryCode;

  @SerializedName("refund_memo")
  String refundMemo;

  @SerializedName("refund_memo_type")
  String refundMemoType;
}
