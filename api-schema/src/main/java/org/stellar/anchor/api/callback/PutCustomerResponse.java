package org.stellar.anchor.api.callback;

import lombok.Data;

/**
 * The response body of the PUT /customer endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
public class PutCustomerResponse {
  String id;
}
