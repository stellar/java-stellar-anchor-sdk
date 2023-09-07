package org.stellar.anchor.api.platform;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The response body of the GET /transactions/{id} endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Platform%20API.yml">Platform
 *     API</a>
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class GetTransactionResponse extends PlatformTransactionData {}
