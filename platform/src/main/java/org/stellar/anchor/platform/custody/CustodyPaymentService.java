package org.stellar.anchor.platform.custody;

import java.time.Instant;
import java.util.List;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails;
import org.stellar.anchor.api.exception.CustodyException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;

/**
 * Abstract payment service. The specific implementation will be used based on selected custody
 * service
 */
public interface CustodyPaymentService {

  /**
   * Generates deposit address and memo for outbound payment
   *
   * @param assetId Stellar asset code
   * @return generated address and memo
   * @throws CustodyException if an error happens on custody service
   * @throws InvalidConfigException if the Stellar asset code doesn't have a mapping to the custody
   *     asset code
   */
  GenerateDepositAddressResponse generateDepositAddress(String assetId)
      throws CustodyException, InvalidConfigException;

  /**
   * Submits outbound transaction payment
   *
   * @param custodyTxn custody transaction
   * @param requestBody additional data, that will be sent in a request to custody service. Can be
   *     used, if, for example, custody service supports some fields in a request specific to it
   * @return external transaction payment ID
   * @throws CustodyException if an error happens on custody service
   * @throws InvalidConfigException if the Stellar asset code doesn't have a mapping to the custody
   *     asset code
   */
  CreateTransactionPaymentResponse createTransactionPayment(
      JdbcCustodyTransaction custodyTxn, String requestBody)
      throws CustodyException, InvalidConfigException;

  /**
   * Get external transaction by id
   *
   * @param txnId external transaction payment ID
   * @return external transaction details
   * @throws CustodyException if an error happens on custody service
   */
  TransactionDetails getTransactionById(String txnId) throws CustodyException;

  /**
   * Get external transactions within time range
   *
   * @param startTime start from time
   * @param endTime to time
   * @return list of external transaction details
   * @throws CustodyException if an error happens on custody service
   */
  List<TransactionDetails> getTransactionsByTimeRange(Instant startTime, Instant endTime)
      throws CustodyException;
}
