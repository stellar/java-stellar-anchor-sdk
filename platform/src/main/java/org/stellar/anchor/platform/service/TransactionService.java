package org.stellar.anchor.platform.service;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.server.data.JdbcSep31Transaction;
import org.stellar.anchor.server.data.JdbcSep31TransactionRepo;
import org.stellar.platform.apis.platform.requests.PatchTransactionRequest;
import org.stellar.platform.apis.platform.requests.PatchTransactionsRequest;
import org.stellar.platform.apis.platform.responses.GetTransactionResponse;
import org.stellar.platform.apis.platform.responses.PatchTransactionsResponse;
import org.stellar.platform.apis.shared.Amount;
import org.stellar.platform.apis.shared.Transaction;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
  private final JdbcSep31TransactionRepo txnRepo;

  TransactionService(JdbcSep31TransactionRepo txnRepo) {
    this.txnRepo = txnRepo;
  }

  public GetTransactionResponse getTransaction(String txnId) throws AnchorException {
    Optional<JdbcSep31Transaction> txn = txnRepo.findById(txnId);
    if (txn.isEmpty()) {
      throw new NotFoundException(String.format("transaction (id=%s) is not found", txnId));
    }

    Transaction txnResponse = fromTransactionToResponse(txn.get());
    GetTransactionResponse response = new GetTransactionResponse();
    BeanUtils.copyProperties(txnResponse, response);

    return response;
  }

  Transaction fromTransactionToResponse(JdbcSep31Transaction txn) {
    return Transaction.builder()
        .id(txn.getId())
        .sep(31)
        .status(txn.getStatus())
        .amountExpected(new Amount(txn.getAmountExpected(), txn.getAmountInAsset()))
        .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
        .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
        .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
        .quoteId(txn.getQuoteId())
        .startedAt(txn.getStartedAt())
        .updatedAt(txn.getUpdatedAt())
        .completedAt(txn.getCompletedAt())
        .transferReceivedAt(txn.getTransferReceivedAt())
        .message(txn.getMessage())
        .externalId(txn.getExternalTransactionId())
        // TODO: Add support for [refunds, stellarTransaction, custodialId, creator]
        .build();
  }

  public PatchTransactionsResponse patchTransactions(PatchTransactionsRequest request) {
    List<PatchTransactionRequest> records = request.getRecords();
    List<Transaction> updatedTxns = new LinkedList<>();
    for (PatchTransactionRequest ptr : records) {
      Optional<JdbcSep31Transaction> optTxn = txnRepo.findById(ptr.getId());
      if (optTxn.isPresent()) {
        JdbcSep31Transaction txn = optTxn.get();
        if (ptr.getStatus() != null) txn.setStatus(ptr.getStatus());
        if (ptr.getAmountIn() != null) {
          txn.setAmountIn(ptr.getAmountIn().getAmount());
          txn.setAmountInAsset(ptr.getAmountIn().getAsset());
        }
        if (ptr.getAmountOut() != null) {
          txn.setAmountOut(ptr.getAmountOut().getAmount());
          txn.setAmountOutAsset(ptr.getAmountOut().getAsset());
        }
        if (ptr.getAmountFee() != null) {
          txn.setAmountFee(ptr.getAmountFee().getAmount());
          txn.setAmountFeeAsset(ptr.getAmountFee().getAsset());
        }
        if (ptr.getTransferReceivedAt() != null)
          txn.setTransferReceivedAt(ptr.getTransferReceivedAt());
        if (ptr.getMessage() != null) txn.setMessage(ptr.getMessage());
        if (ptr.getExternalTransactionId() != null)
          txn.setExternalTransactionId(ptr.getExternalTransactionId());
        // TODO: Update [refunds] field

        txnRepo.save(txn);

        updatedTxns.add(fromTransactionToResponse(txn));
      }
    }
    return new PatchTransactionsResponse(updatedTxns);
  }
}
