package org.stellar.anchor.platform.service;

import static org.stellar.anchor.platform.utils.TransactionHelper.toCustodyTransaction;
import static org.stellar.anchor.util.Log.debugF;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.CustodyException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.custody.CustodyBadRequestException;
import org.stellar.anchor.api.exception.custody.CustodyNotFoundException;
import org.stellar.anchor.api.exception.custody.CustodyServiceUnavailableException;
import org.stellar.anchor.api.exception.custody.CustodyTooManyRequestsException;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.platform.apiclient.CustodyApiClient;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public class CustodyServiceImpl implements CustodyService {

  private final Optional<CustodyApiClient> custodyApiClient;

  public CustodyServiceImpl(Optional<CustodyApiClient> custodyApiClient) {
    this.custodyApiClient = custodyApiClient;
  }

  @Override
  public void createTransaction(Sep24Transaction txn) throws AnchorException {
    create(toCustodyTransaction(txn));
  }

  @Override
  public void createTransaction(Sep31Transaction txn) throws AnchorException {
    create(toCustodyTransaction(txn));
  }

  private void create(CreateCustodyTransactionRequest request)
      throws CustodyException, InvalidConfigException {
    if (custodyApiClient.isEmpty()) {
      // custody.type is set to 'none'
      throw new InvalidConfigException("Integration with custody service is not enabled");
    }
    custodyApiClient.get().createTransaction(request);
  }

  @Override
  public CreateTransactionPaymentResponse createTransactionPayment(String txnId, String requestBody)
      throws AnchorException {
    if (custodyApiClient.isEmpty()) {
      // custody.type is set to 'none'
      throw new InvalidConfigException("Integration with custody service is not enabled");
    }

    CreateTransactionPaymentResponse response;
    try {
      response = custodyApiClient.get().createTransactionPayment(txnId, requestBody);
    } catch (CustodyException e) {
      switch (HttpStatus.valueOf(e.getStatusCode())) {
        case NOT_FOUND:
          throw new CustodyNotFoundException(e.getRawMessage());
        case TOO_MANY_REQUESTS:
          throw new CustodyTooManyRequestsException(e.getRawMessage());
        case BAD_REQUEST:
          throw new CustodyBadRequestException(e.getRawMessage());
        case SERVICE_UNAVAILABLE:
          throw new CustodyServiceUnavailableException(e.getRawMessage());
        default:
          debugF("Unhandled status code (%s)", e.getStatusCode());
          throw e;
      }
    }

    return response;
  }
}
