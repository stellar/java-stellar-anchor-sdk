package org.stellar.anchor.api.callback;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;

/**
 * The response body of the GET /fee endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
@AllArgsConstructor
public class GetFeeResponse {
  Amount fee;
}
