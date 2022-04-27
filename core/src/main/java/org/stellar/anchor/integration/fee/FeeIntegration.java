package org.stellar.anchor.integration.fee;

import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.callback.GetFeeResponse;
import org.stellar.anchor.api.exception.AnchorException;

public interface FeeIntegration {
  GetFeeResponse getFee(GetFeeRequest request) throws AnchorException;
}
