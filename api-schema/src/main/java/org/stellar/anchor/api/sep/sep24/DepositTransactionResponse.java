package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The response body of the POST /transactions/deposit/interactive endpoint of SEP-24.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0024.md#fields">Refer
 *     to SEP-24</a>
 */
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
