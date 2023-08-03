package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the PATCH /transactions endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Data
@Builder
public class PatchTransactionsRequest {
  List<PatchTransactionRequest> records;
}
