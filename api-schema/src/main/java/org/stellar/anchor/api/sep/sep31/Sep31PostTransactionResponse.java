package org.stellar.anchor.api.sep.sep31;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The response body of the POST /transactions endpoint of SEP-31.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0031.md#fields">Refer
 *     to SEP-31</a>
 */
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
