package org.stellar.anchor.integration.fee;

import org.stellar.anchor.exception.AnchorException;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;

public interface FeeIntegration {
  GetFeeResponse getFee(GetFeeRequest request) throws AnchorException;
}
