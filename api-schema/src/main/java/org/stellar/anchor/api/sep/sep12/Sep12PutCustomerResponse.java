package org.stellar.anchor.api.sep.sep12;

import lombok.Builder;
import lombok.Data;

/**
 * The response to the PUT /customer endpoint of SEP-12.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields">Refer
 *     to SEP-12</a>
 */
@Data
@Builder
public class Sep12PutCustomerResponse {
  String id;
}
