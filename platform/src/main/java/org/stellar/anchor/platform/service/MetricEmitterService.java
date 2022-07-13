package org.stellar.anchor.platform.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import org.stellar.anchor.config.MetricConfig;
import org.stellar.anchor.platform.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.util.Log;

public class MetricEmitterService {
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
  private final JdbcSep31TransactionRepo sep31TransactionStore;
  AtomicInteger pendingStellarTxns = new AtomicInteger(0);
  AtomicInteger pendingCustomerInfoUpdateTxns = new AtomicInteger(0);
  AtomicInteger pendingSenderTxns = new AtomicInteger(0);
  AtomicInteger pendingReceiverTxns = new AtomicInteger(0);
  AtomicInteger pendingExternalTxns = new AtomicInteger(0);
  AtomicInteger completedTxns = new AtomicInteger(0);
  AtomicInteger errorTxns = new AtomicInteger(0);

  public MetricEmitterService(
      MetricConfig metricConfig, JdbcSep31TransactionRepo sep31TransactionRepo) {
    this.sep31TransactionStore = sep31TransactionRepo;
    // Create counters
    Metrics.counter("sep31.transaction", "status", "pending_stellar");
    Metrics.counter("sep31.transaction", "status", "pending_customer_info_update");
    Metrics.counter("sep31.transaction", "status", "pending_sender");
    Metrics.counter("sep31.transaction", "status", "pending_receiver");
    Metrics.counter("sep31.transaction", "status", "pending_external");
    Metrics.counter("sep31.transaction", "status", "completed");
    Metrics.counter("sep31.transaction", "status", "error");
    Metrics.counter("logger", "type", "warn");
    Metrics.counter("logger", "type", "error");

    // create gauges for SEP-31 Transactions - .db indicates that the metrics is pulled from the
    // database
    Metrics.gauge("sep31.transaction.db", Tags.of("status", "pending_stellar"), pendingStellarTxns);
    Metrics.gauge(
        "sep31.transaction.db",
        Tags.of("status", "pending_customer_info_update"),
        pendingCustomerInfoUpdateTxns);
    Metrics.gauge("sep31.transaction.db", Tags.of("status", "pending_sender"), pendingSenderTxns);
    Metrics.gauge(
        "sep31.transaction.db", Tags.of("status", "pending_receiver"), pendingReceiverTxns);
    Metrics.gauge(
        "sep31.transaction.db", Tags.of("status", "pending_external"), pendingExternalTxns);
    Metrics.gauge("sep31.transaction.db", Tags.of("status", "completed"), completedTxns);
    Metrics.gauge("sep31.transaction.db", Tags.of("status", "error"), errorTxns);

    // TODO add gauges for SEP-24 Transactions

    if (metricConfig.isOptionalMetricsEnabled()) {
      this.executor.scheduleAtFixedRate(
          new MetricEmitter(), 0, metricConfig.getRunInterval(), TimeUnit.SECONDS);
    }
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
        errorTxns.set(sep31TransactionStore.findByStatusCount("error"));
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
