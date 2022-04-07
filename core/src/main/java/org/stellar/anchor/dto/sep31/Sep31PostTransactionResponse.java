package org.stellar.anchor.dto.sep31;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sep31PostTransactionResponse {
  String id;

  @SerializedName("stellar_account_id")
  String stellarAccountId;

  @SerializedName("stellar_memo_type")
  String stellarMemoType;

  @SerializedName("stellar_memo")
  String stellarMemo;
}
