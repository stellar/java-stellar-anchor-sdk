package org.stellar.anchor.platform.job;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.errorEx;
import static org.stellar.anchor.util.Log.info;

import java.io.IOException;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.stellar.anchor.api.custody.fireblocks.TransactionDetails;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.custody.CustodyPayment;
import org.stellar.anchor.platform.custody.CustodyPaymentService;
import org.stellar.anchor.platform.custody.CustodyTransactionService;
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService;
import org.stellar.anchor.platform.data.CustodyTransactionStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;

public class FireblocksTransactionsReconciliationJob {

  private final FireblocksConfig fireblocksConfig;
  private final CustodyPaymentService custodyPaymentService;
  private final FireblocksEventService fireblocksEventService;
  private final CustodyTransactionService custodyTransactionService;

  public FireblocksTransactionsReconciliationJob(
      FireblocksConfig fireblocksConfig,
      CustodyPaymentService custodyPaymentService,
      FireblocksEventService fireblocksEventService,
      CustodyTransactionService custodyTransactionService) {
    this.fireblocksConfig = fireblocksConfig;
    this.custodyPaymentService = custodyPaymentService;
    this.fireblocksEventService = fireblocksEventService;
    this.custodyTransactionService = custodyTransactionService;
  }

  @Scheduled(cron = "${custody.fireblocks.reconciliation.cron_expression}")
  public void reconcileTransactions() {
    info("Fireblocks Transaction Reconciliation job started");

    custodyTransactionService
        .getTransactionsEligibleForReconciliation()
        .forEach(this::reconcileTransaction);

    info("Fireblocks Transaction Reconciliation job finished");
  }

  private void reconcileTransaction(JdbcCustodyTransaction transaction) {
    try {
      TransactionDetails fireblocksTransaction =
          custodyPaymentService.getTransactionById(transaction.getExternalTxId());

      int attempt = transaction.getReconciliationAttemptCount() + 1;

      if (fireblocksTransaction.getStatus().isObservable()) {
        debugF(
            "Reconciliation attempt #[{}]: Fireblocks transaction status changed to [{}]",
            attempt,
            fireblocksTransaction.getStatus().name());
        Optional<CustodyPayment> payment = fireblocksEventService.convert(fireblocksTransaction);
        if (payment.isPresent()) {
          fireblocksEventService.handlePayment(payment.get());
        }
      } else {
        debugF(
            "Reconciliation attempt #[{}]: Fireblocks transaction status wasn't changed", attempt);

        transaction.setReconciliationAttemptCount(attempt);
        if (transaction.getReconciliationAttemptCount()
            >= fireblocksConfig.getReconciliation().getMaxAttempts()) {
          debugF("Change transaction [{}] status to FAILED", transaction.getId());
          transaction.setStatus(CustodyTransactionStatus.FAILED.toString());
        }
        custodyTransactionService.updateCustodyTransaction(transaction);
      }
    } catch (AnchorException | IOException e) {
      errorEx(
          String.format("Failed to reconcile status for transaction (id=%s)", transaction.getId()),
          e);
    }
  }
}
