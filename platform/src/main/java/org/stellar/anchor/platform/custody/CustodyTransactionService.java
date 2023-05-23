package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.platform.data.CustodyTransactionStatus.CREATED;
import static org.stellar.anchor.platform.data.CustodyTransactionStatus.SUBMITTED;
import static org.stellar.anchor.util.Log.debugF;

import java.time.Instant;
import java.util.List;
import java.util.Set;
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

  private final CustodyPaymentService custodyPaymentService;
  private final JdbcCustodyTransactionRepo custodyTransactionRepo;

  public CustodyTransactionService(
      JdbcCustodyTransactionRepo custodyTransactionRepo,
      CustodyPaymentService custodyPaymentService) {
    this.custodyTransactionRepo = custodyTransactionRepo;
    this.custodyPaymentService = custodyPaymentService;
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
            .amount(request.getAmount())
            .amountAsset(request.getAmountAsset())
            .kind(request.getKind())
            .reconciliationAttemptCount(0)
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
      response = custodyPaymentService.createTransactionPayment(txn, requestBody);
      updateCustodyTransaction(txn, response.getId(), SUBMITTED);
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
    updateCustodyTransaction(txn);
  }

  public void updateCustodyTransaction(JdbcCustodyTransaction txn) {
    txn.setUpdatedAt(Instant.now());
    custodyTransactionRepo.save(txn);
  }

  public List<JdbcCustodyTransaction> getOutboundTransactionsEligibleForReconciliation() {
    return custodyTransactionRepo.findAllByStatusAndExternalTxIdNotNull(SUBMITTED.toString());
  }

  public List<JdbcCustodyTransaction> getInboundTransactionsEligibleForReconciliation() {
    return custodyTransactionRepo.findAllByStatusAndKindIn(
        CREATED.toString(), Set.of(RECEIVE.getKind(), WITHDRAWAL.getKind()));
  }
}
