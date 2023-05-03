package org.stellar.anchor.platform.service;

import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_SENDER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.platform.utils.TransactionHelper.toCustodyTransaction;

import java.util.Optional;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.CustodyException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.custody.CustodyTransactionService;
import org.stellar.anchor.platform.apiclient.CustodyApiClient;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public class CustodyTransactionServiceImpl implements CustodyTransactionService {

  private final Optional<CustodyApiClient> custodyApiClient;

  public CustodyTransactionServiceImpl(Optional<CustodyApiClient> custodyApiClient) {
    this.custodyApiClient = custodyApiClient;
  }

  @Override
  public void create(Sep24Transaction txn) throws AnchorException {
    if ((Kind.DEPOSIT.getKind().equals(txn.getKind())
            && PENDING_ANCHOR.toString().equals(txn.getStatus()))
        || (Kind.WITHDRAWAL.getKind().equals(txn.getKind())
            && PENDING_USR_TRANSFER_START.toString().equals(txn.getStatus()))) {
      create(toCustodyTransaction(txn));
    }
  }

  @Override
  public void create(Sep31Transaction txn) throws AnchorException {
    if (PENDING_SENDER.toString().equals(txn.getStatus())) {
      create(toCustodyTransaction(txn));
    }
  }

  private void create(CreateCustodyTransactionRequest request) throws CustodyException {
    if (custodyApiClient.isPresent()) {
      custodyApiClient.get().createTransaction(request);
    }
  }
}
