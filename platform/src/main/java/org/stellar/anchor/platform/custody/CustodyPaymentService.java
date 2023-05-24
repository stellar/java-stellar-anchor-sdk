package org.stellar.anchor.platform.custody;

import java.time.Instant;
import java.util.List;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;

public interface CustodyPaymentService {

  GenerateDepositAddressResponse generateDepositAddress(String assetId)
      throws FireblocksException, InvalidConfigException;

  CreateTransactionPaymentResponse createTransactionPayment(
      JdbcCustodyTransaction custodyTxn, String requestBody)
      throws FireblocksException, InvalidConfigException;

  TransactionDetails getTransactionById(String txnId) throws FireblocksException;

  List<TransactionDetails> getTransactionsByTimeRange(Instant startTime, Instant endTime)
      throws FireblocksException;
}
