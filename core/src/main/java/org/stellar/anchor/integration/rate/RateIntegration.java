package org.stellar.anchor.integration.rate;

import org.stellar.anchor.exception.AnchorException;

public interface RateIntegration {
  GetRateResponse getRate(GetRateRequest request) throws AnchorException;
}
