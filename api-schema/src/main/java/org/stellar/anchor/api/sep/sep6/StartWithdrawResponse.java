package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The response to the GET /withdraw endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#response-1">GET
 *     /withdraw response</a>
 */
@Builder
@Data
public class StartWithdrawResponse {
  /** The account the user should send its token back to. */
  @SerializedName("account_id")
  String accountId;

  /** Value of memo to attach to transaction. */
  String memo;

  /** Type of memo to attach to transaction. */
  @SerializedName("memo_type")
  String memoType;

  /** The anchor's ID for this withdrawal. */
  String id;
}
