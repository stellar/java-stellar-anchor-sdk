package org.stellar.anchor.platform.service;

import java.time.Instant;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.platform.data.CustodyTransactionStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

public class CustodyTransactionService {

  private final JdbcCustodyTransactionRepo custodyTransactionRepo;

  public CustodyTransactionService(JdbcCustodyTransactionRepo custodyTransactionRepo) {
    this.custodyTransactionRepo = custodyTransactionRepo;
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
            .requestAssetCode(request.getRequestAssetCode())
            .requestAssetIssuer(request.getRequestAssetIssuer())
            .build());
  }
}
