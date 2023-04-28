package org.stellar.anchor.platform.custody;

import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.exception.FireblocksException;

public interface PaymentService {

  GenerateDepositAddressResponse createNewDepositAddress(String assetId) throws FireblocksException;
}
