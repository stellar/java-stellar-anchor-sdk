package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The response body of the GET /transactions endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetTransactionsResponse {
  List<GetTransactionResponse> records;
}
