package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;

/**
 * The interface for the rate endpoint of the callback API.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a> *
 */
public interface RateIntegration {
  /**
   * Gets a rate.
   *
   * @param request The request to get a rate.
   * @return the GET rate response.
   * @throws AnchorException if error happens
   */
  GetRateResponse getRate(GetRateRequest request) throws AnchorException;
}
