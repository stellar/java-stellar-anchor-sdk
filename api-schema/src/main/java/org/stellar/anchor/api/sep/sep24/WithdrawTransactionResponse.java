package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** The response body of the POST /transactions/withdraw/interactive endpoint of SEP-24. */
@EqualsAndHashCode(callSuper = true)
@Data
public class WithdrawTransactionResponse extends TransactionResponse {
  @SerializedName("withdraw_anchor_account")
  String withdrawAnchorAccount;

  @SerializedName("withdraw_memo")
  String withdrawMemo;

  @SerializedName("withdraw_memo_type")
  String withdrawMemoType;
}
