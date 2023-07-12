package org.stellar.anchor.platform.job;

import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.util.Log.info;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.config.PropertyCustodyConfig;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSep31Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrust;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class TrustlineCheckJob {

  private final Horizon horizon;
  private final JdbcTransactionPendingTrustRepo transactionPendingTrustRepo;
  private final PropertyCustodyConfig custodyConfig;
  private final TransactionService transactionService;
  private final Sep24TransactionStore txn24Store;
  private final Sep31TransactionStore txn31Store;
  private final CustodyService custodyService;

  public TrustlineCheckJob(
      Horizon horizon,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo,
      PropertyCustodyConfig custodyConfig,
      TransactionService transactionService,
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      CustodyService custodyService) {
    this.horizon = horizon;
    this.transactionPendingTrustRepo = transactionPendingTrustRepo;
    this.custodyConfig = custodyConfig;
    this.transactionService = transactionService;
    this.txn24Store = txn24Store;
    this.txn31Store = txn31Store;
    this.custodyService = custodyService;
  }

  @Scheduled(cron = "${custody.trustline.check_cron_expression}")
  public void checkTrust() throws AnchorException {
    info("Trustline Check job started");

    for (JdbcTransactionPendingTrust t : transactionPendingTrustRepo.findAll()) {
      if (isCheckTimedOut(t)) {
        JdbcSepTransaction txn = transactionService.findTransaction(t.getId());
        txn.setUpdatedAt(Instant.now());
        txn.setStatus(PENDING_ANCHOR.toString());

        switch (Sep.from(txn.getProtocol())) {
          case SEP_24:
            JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
            txn24.setMessage(custodyConfig.getTrustline().getTimeoutMessage());
            txn24Store.save(txn24);
            break;
          case SEP_31:
            JdbcSep31Transaction txn31 = (JdbcSep31Transaction) txn;
            txn31.setRequiredInfoMessage(custodyConfig.getTrustline().getTimeoutMessage());
            txn31Store.save(txn31);
            break;
        }

        transactionPendingTrustRepo.delete(t);
      } else {
        boolean trustlineConfigured = horizon.isTrustlineConfigured(t.getAccount(), t.getAsset());
        if (trustlineConfigured) {
          custodyService.createTransactionPayment(t.getId(), null);
          transactionPendingTrustRepo.delete(t);
        } else {
          transactionPendingTrustRepo.save(t);
        }
      }
    }

    info("Trustline Check job finished");
  }

  private boolean isCheckTimedOut(JdbcTransactionPendingTrust trust) {
    return trust
        .getCreatedAt()
        .plus(custodyConfig.getTrustline().getCheckDuration(), ChronoUnit.MINUTES)
        .isBefore(Instant.now());
  }
}
