package org.stellar.anchor.platform.custody;

import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;

public interface CustodyPaymentService {

  GenerateDepositAddressResponse generateDepositAddress(String assetId) throws FireblocksException;

  CreateTransactionPaymentResponse createTransactionPayment(
      JdbcCustodyTransaction custodyTxn, String requestBody) throws FireblocksException;
}
