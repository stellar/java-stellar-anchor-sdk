package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;

/**
 * The interface for the unique address endpoint of the callback API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Callbacks%20API.yml">Callback
 *     API</a>
 */
public interface UniqueAddressIntegration {
  /**
   * Gets the unique address of a transaction to which the Stellar fund can be sent.
   *
   * @param transactionId the transaction ID
   * @return The GetUniqueAddressResponse
   */
  GetUniqueAddressResponse getUniqueAddress(String transactionId) throws AnchorException;
}
