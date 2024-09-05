package org.stellar.anchor.api.callback;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * The response body of the PUT /customer endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class PutCustomerResponse extends GetCustomerResponse {}
