package org.stellar.anchor.platform.service;

import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;

import io.micrometer.core.instrument.Tags;
import org.stellar.anchor.platform.data.JdbcSep31TransactionStore;
import org.stellar.anchor.util.Log;

public class MetricEmitterService {
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
  private final JdbcSep31TransactionStore sep31TransactionStore;
  AtomicInteger pendingStellarTxns = new AtomicInteger(0);
  AtomicInteger pendingCustomerInfoUpdateTxns = new AtomicInteger(0);
  AtomicInteger pendingSenderTxns = new AtomicInteger(0);
  AtomicInteger pendingReceiverTxns = new AtomicInteger(0);
  AtomicInteger pendingExternalTxns = new AtomicInteger(0);
  AtomicInteger completedTxns = new AtomicInteger(0);

  public MetricEmitterService(JdbcSep31TransactionStore sep31TransactionStore) {
    this.sep31TransactionStore = sep31TransactionStore;
    this.executor.scheduleAtFixedRate(new MetricEmitter(), 0, 30, TimeUnit.SECONDS);
    // create gauges for SEP-31 Transactions
    Metrics.gauge("sep31.transaction", Tags.of("status", "pendingStellar"), pendingStellarTxns);
    Metrics.gauge("sep31.transaction", Tags.of("status", "pendingCustomerInfoUpdate"), pendingCustomerInfoUpdateTxns);
    Metrics.gauge("sep31.transaction", Tags.of("status", "pendingSender"), pendingSenderTxns);
    Metrics.gauge("sep31.transaction", Tags.of("status", "pendingReceiver"), pendingReceiverTxns);
    Metrics.gauge("sep31.transaction", Tags.of("status", "pendingExternal"), pendingExternalTxns);
    Metrics.gauge("sep31.transaction", Tags.of("status", "completed"), completedTxns);

    // TODO add gauges for SEP-24 Transactions

  }

  class MetricEmitter implements Runnable {
    public void run() {
      try {
        pendingStellarTxns.set(sep31TransactionStore.findByStatusCount("pending_stellar"));
        pendingCustomerInfoUpdateTxns.set(
            sep31TransactionStore.findByStatusCount("pending_customer_info_update"));
        pendingSenderTxns.set(sep31TransactionStore.findByStatusCount("pending_sender"));
        pendingReceiverTxns.set(sep31TransactionStore.findByStatusCount("pending_receiver"));
        pendingExternalTxns.set(sep31TransactionStore.findByStatusCount("pending_external"));
        completedTxns.set(sep31TransactionStore.findByStatusCount("completed"));
      } catch (Exception ex) {
        Log.errorEx(ex);
      }
    }
  }

  public void stop() {
    executor.shutdownNow();
  }

  @PreDestroy
  public void destroy() {
    stop();
  }
}
