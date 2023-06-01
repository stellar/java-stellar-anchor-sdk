package org.stellar.anchor.api.platform;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The response body of the GET /transactions/{id} endpoint of the Platform API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Platform%20API.yml">Platform
 *     API</a>
 */
@SuperBuilder
@NoArgsConstructor
public class GetTransactionResponse extends PlatformTransactionData {}
