package org.stellar.anchor.api.sep.sep6;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The response to the GET /transaction endpoint of SEP-6.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#transaction-history">SEP-6
 *     Transaction History</a>
 */
@Data
@AllArgsConstructor
public class GetTransactionsResponse {
  List<Sep6Transaction> transactions;
}
