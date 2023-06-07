package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.util.Log.debugF;

import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.api.exception.custody.CustodyBadRequestException;
import org.stellar.anchor.api.exception.custody.CustodyNotFoundException;
import org.stellar.anchor.api.exception.custody.CustodyServiceUnavailableException;
import org.stellar.anchor.api.exception.custody.CustodyTooManyRequestsException;
import org.stellar.anchor.platform.data.CustodyTransactionStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

public class CustodyTransactionService {

  private final PaymentService paymentService;
  private final JdbcCustodyTransactionRepo custodyTransactionRepo;

  public CustodyTransactionService(
      JdbcCustodyTransactionRepo custodyTransactionRepo, PaymentService paymentService) {
    this.custodyTransactionRepo = custodyTransactionRepo;
    this.paymentService = paymentService;
  }

  public void create(CreateCustodyTransactionRequest request) {
    custodyTransactionRepo.save(
        JdbcCustodyTransaction.builder()
            .id(request.getId())
            .status(CustodyTransactionStatus.CREATED.toString())
            .createdAt(Instant.now())
            .memo(request.getMemo())
            .memoType(request.getMemoType())
            .protocol(request.getProtocol())
            .fromAccount(request.getFromAccount())
            .toAccount(request.getToAccount())
            .amountIn(request.getAmountIn())
            .amountInAsset(request.getAmountInAsset())
            .amountOut(request.getAmountOut())
            .amountOutAsset(request.getAmountOutAsset())
            .kind(request.getKind())
            .build());
  }

  public CreateTransactionPaymentResponse createPayment(String txnId, String requestBody)
      throws AnchorException {
    JdbcCustodyTransaction txn = custodyTransactionRepo.findById(txnId).orElse(null);
    if (txn == null) {
      throw new CustodyNotFoundException(String.format("Transaction (id=%s) is not found", txnId));
    }

    CreateTransactionPaymentResponse response;
    try {
      response = paymentService.createTransactionPayment(txn, requestBody);
      updateCustodyTransaction(txn, response.getId(), CustodyTransactionStatus.SUBMITTED);
    } catch (FireblocksException e) {
      updateCustodyTransaction(txn, StringUtils.EMPTY, CustodyTransactionStatus.FAILED);
      switch (HttpStatus.valueOf(e.getStatusCode())) {
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

  private void updateCustodyTransaction(
      JdbcCustodyTransaction txn, String externalTransactionId, CustodyTransactionStatus status) {
    txn.setExternalTxId(externalTransactionId);
    txn.setStatus(status.toString());
    txn.setUpdatedAt(Instant.now());
    custodyTransactionRepo.save(txn);
  }
}