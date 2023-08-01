package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;

/**
 * The response body of the PATCH /transactions endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Data
public class PatchTransactionsResponse {
  List<GetTransactionResponse> records;

  public PatchTransactionsResponse(List<GetTransactionResponse> records) {
    this.records = records;
  }
}
