package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;

/**
 * Interface for the fee endpoint of the callback API
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Callbacks%20API.yml">Callback
 *     API</a>
 */
public interface FeeIntegration {
  GetFeeResponse getFee(GetFeeRequest request) throws AnchorException;
}
