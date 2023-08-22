package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the GET /transaction endpoint of SEP-6.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#transaction-history">SEP-6
 *     Transaction History</a>
 */
@Builder
@Data
public class GetTransactionRequest {
  String id;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String lang;
}
