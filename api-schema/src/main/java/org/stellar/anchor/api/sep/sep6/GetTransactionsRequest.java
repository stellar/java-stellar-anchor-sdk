package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the GET /transactions endpoint of SEP-6.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#transaction-history">SEP-6
 *     Transaction History</a>
 */
@Builder
@Data
public class GetTransactionsRequest {
  @SerializedName("asset_code")
  String assetCode;

  String account;

  @SerializedName("no_older_than")
  String noOlderThan;

  Integer limit;

  String kind;

  @SerializedName("paging_id")
  String pagingId;

  String lang;
}
