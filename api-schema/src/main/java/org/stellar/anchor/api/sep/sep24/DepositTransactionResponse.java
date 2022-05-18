package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DepositTransactionResponse extends TransactionResponse {
  @SerializedName("deposit_memo")
  String depositMemo;

  @SerializedName("deposit_memo_type")
  String depositMemoType;

  @SerializedName("claimable_balance_id")
  String claimableBalanceId;
}
