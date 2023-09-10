package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetWithdrawResponse {
  @SerializedName("account_id")
  String accountId;

  String memo;

  @SerializedName("memo_type")
  String memoType;

  String id;
}
