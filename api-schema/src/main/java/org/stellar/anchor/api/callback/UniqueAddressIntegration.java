package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.ServiceUnavailableException;

public interface UniqueAddressIntegration {
  /**
   * Gets the unique address of a transaction to which the Stellar fund can be sent.
   * @param transactionId
   * @return The GetUniqueAddressResponse
   */
  GetUniqueAddressResponse getUniqueAddressResponse(String transactionId) throws AnchorException;
}
