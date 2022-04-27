package org.stellar.anchor.integration.rate;

import org.stellar.anchor.api.exception.AnchorException;

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
