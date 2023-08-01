package org.stellar.anchor.api.platform;

import lombok.*;

/**
 * The request body of the PATCH /transactions/{id} endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatchTransactionRequest {
  PlatformTransactionData transaction;
}
