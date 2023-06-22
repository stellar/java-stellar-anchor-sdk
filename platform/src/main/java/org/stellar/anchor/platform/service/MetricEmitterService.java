package org.stellar.anchor.platform.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.stellar.anchor.platform.config.MetricConfig;
import org.stellar.anchor.platform.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.platform.utils.DaemonExecutors;
import org.stellar.anchor.util.Log;

public class MetricEmitterService {
  private final ScheduledExecutorService executor = DaemonExecutors.newScheduledThreadPool(1);
  private final MetricConfig metricConfig;
  private final JdbcSep31TransactionRepo sep31TransactionStore;
  final AtomicInteger pendingStellarTxns = new AtomicInteger(0);
  final AtomicInteger pendingCustomerInfoUpdateTxns = new AtomicInteger(0);
  final AtomicInteger pendingSenderTxns = new AtomicInteger(0);
  final AtomicInteger pendingReceiverTxns = new AtomicInteger(0);
  final AtomicInteger pendingExternalTxns = new AtomicInteger(0);
  final AtomicInteger completedTxns = new AtomicInteger(0);
  final AtomicInteger errorTxns = new AtomicInteger(0);

  public MetricEmitterService(
      MetricConfig metricConfig, JdbcSep31TransactionRepo sep31TransactionRepo) {
    this.metricConfig = metricConfig;
    this.sep31TransactionStore = sep31TransactionRepo;
    // Create counters
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_PENDING_STELLAR.toString());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_PENDING_CUSTOMER.toString());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_PENDING_SENDER.name());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_PENDING_RECEIVER.toString());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_PENDING_EXTERNAL.toString());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_COMPLETED.toString());
    Metrics.counter(
        AnchorMetrics.SEP31_TRANSACTION.toString(),
        "status",
        AnchorMetrics.TAG_SEP31_STATUS_ERROR.toString());
    Metrics.counter(AnchorMetrics.LOGGER.toString(), "type", "warn");
    Metrics.counter(AnchorMetrics.LOGGER.toString(), "type", "error");

    // create gauges for SEP-31 Transactions - .db indicates that the metric is pulled from the
    // database
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_PENDING_STELLAR.toString()),
        pendingStellarTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_PENDING_CUSTOMER.toString()),
        pendingCustomerInfoUpdateTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_PENDING_SENDER.name()),
        pendingSenderTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_PENDING_RECEIVER.toString()),
        pendingReceiverTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_PENDING_EXTERNAL.toString()),
        pendingExternalTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_COMPLETED.toString()),
        completedTxns);
    Metrics.gauge(
        AnchorMetrics.SEP31_TRANSACTION_DB.toString(),
        Tags.of("status", AnchorMetrics.TAG_SEP31_STATUS_ERROR.toString()),
        errorTxns);

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
        errorTxns.set(sep31TransactionStore.findByStatusCount("error"));
      } catch (Exception ex) {
        Log.errorEx(ex);
      }
    }
  }

  @PreDestroy
  public void stop() {
    executor.shutdownNow();
  }

  @PostConstruct
  public void start() {
    if (metricConfig != null) {
      if (metricConfig.isExtrasEnabled()) {
        this.executor.scheduleAtFixedRate(
            new MetricEmitter(), 10, metricConfig.getRunInterval(), TimeUnit.SECONDS);
      }
    }
  }
}
