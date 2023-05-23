package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.util.Log.infoF;
import static org.stellar.anchor.util.Log.warn;

import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.platform.custody.CustodyPayment.CustodyPaymentStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;
import org.stellar.anchor.platform.service.AnchorMetrics;

/** Custody payment handler for SEP31 transactions */
public class Sep31CustodyPaymentHandler extends CustodyPaymentHandler {

  public Sep31CustodyPaymentHandler(
      JdbcCustodyTransactionRepo custodyTransactionRepo, PlatformApiClient platformApiClient) {
    super(custodyTransactionRepo, platformApiClient);
  }

  @Override
  public void onReceived(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException {
    infoF(
        "Incoming inbound payment for SEP-31 transaction. Payment: id[{}], externalTxId[{}]",
        payment.getId(),
        payment.getExternalTxId());

    validatePayment(txn, payment);

    SepTransactionStatus newSepTxnStatus = getSepWithdrawalTransactionStatus(payment.getStatus());
    updateTransaction(txn, payment, newSepTxnStatus);

    Metrics.counter(
            AnchorMetrics.SEP31_TRANSACTION.toString(), "status", newSepTxnStatus.toString())
        .increment();
    Metrics.counter(AnchorMetrics.PAYMENT_RECEIVED.toString(), "asset", payment.getAssetName())
        .increment(Double.parseDouble(payment.getAmount()));
  }

  @Override
  public void onSent(JdbcCustodyTransaction tx, CustodyPayment payment) {
    warn("Outbound payments are not implemented for SEP31");
  }

  private SepTransactionStatus getSepWithdrawalTransactionStatus(
      CustodyPaymentStatus custodyPaymentStatus) {
    switch (custodyPaymentStatus) {
      case SUCCESS:
        return SepTransactionStatus.PENDING_RECEIVER;
      case ERROR:
        return SepTransactionStatus.ERROR;
      default:
        throw new RuntimeException(
            String.format("Unsupported custody transaction status[%s]", custodyPaymentStatus));
    }
  }
}
