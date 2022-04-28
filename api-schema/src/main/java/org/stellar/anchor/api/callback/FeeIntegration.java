package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;

public interface FeeIntegration {
  GetFeeResponse getFee(GetFeeRequest request) throws AnchorException;
}
