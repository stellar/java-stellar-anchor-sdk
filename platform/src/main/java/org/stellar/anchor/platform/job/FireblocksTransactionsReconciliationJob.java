package org.stellar.anchor.platform.job;

import static org.stellar.anchor.util.Log.info;

import org.springframework.scheduling.annotation.Scheduled;

public class FireblocksTransactionsReconciliationJob {

  @Scheduled(cron = "${custody.fireblocks.transactions_reconciliation_cron}")
  public void reconcileTransaction() {
    info("Reconcile Transactions Scheduled job called");
    // Placeholder for transactions reconciliation job
    // TODO: Add business logic to get transactions from Fireblocks, compare it with data in DB and
    // reconcile data
  }
}
