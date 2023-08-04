package org.stellar.anchor.api.sep.sep6;

import lombok.Builder;
import lombok.Data;

/**
 * The response to the GET /deposit endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md#response">GET
 *     /deposit response</a>
 */
@Builder
@Data
public class GetDepositResponse {
  /**
   * Terse but complete instructions for how to deposit the asset.
   *
   * <p>Anchor Platform does not support synchronous deposit flows, so this field will never contain
   * real instructions.
   */
  String how;

  /** The anchor's ID for this deposit. */
  String id;
}
