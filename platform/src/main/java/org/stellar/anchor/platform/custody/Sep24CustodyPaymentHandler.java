package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.util.Log.infoF;

import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.platform.custody.CustodyPayment.CustodyPaymentStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;
import org.stellar.anchor.platform.service.AnchorMetrics;

public class Sep24CustodyPaymentHandler extends CustodyPaymentHandler {

  public Sep24CustodyPaymentHandler(
      JdbcCustodyTransactionRepo custodyTransactionRepo, PlatformApiClient platformApiClient) {
    super(custodyTransactionRepo, platformApiClient);
  }

  @Override
  public void onReceived(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException {
    infoF(
        "Incoming inbound payment for SEP-24 transaction. Payment: id[{}], externalTxId[{}]",
        payment.getId(),
        payment.getExternalTxId());

    if (CustodyPaymentStatus.SUCCESS == payment.getStatus()) {
      validatePayment(txn, payment);
    }

    SepTransactionStatus newSepTxnStatus = getSepWithdrawalTransactionStatus(payment.getStatus());
    updateTransaction(txn, payment, newSepTxnStatus);

    Metrics.counter(
            AnchorMetrics.SEP24_TRANSACTION.toString(), "status", newSepTxnStatus.toString())
        .increment();
    Metrics.counter(AnchorMetrics.PAYMENT_RECEIVED.toString(), "asset", payment.getAssetName())
        .increment(Double.parseDouble(payment.getAmount()));
  }

  @Override
  public void onSent(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException {
    infoF(
        "Incoming outbound payment for SEP-24 transaction. Payment: id[{}], externalTxId[{}]",
        payment.getId(),
        payment.getExternalTxId());

    if (CustodyPaymentStatus.SUCCESS == payment.getStatus()) {
      validatePayment(txn, payment);
    }

    SepTransactionStatus newSepTxnStatus = getSepDepositTransactionStatus(payment.getStatus());
    updateTransaction(txn, payment, newSepTxnStatus);

    Metrics.counter(
            AnchorMetrics.SEP24_TRANSACTION.toString(), "status", newSepTxnStatus.toString())
        .increment();
    Metrics.counter(AnchorMetrics.PAYMENT_SENT.toString(), "asset", payment.getAssetName())
        .increment(Double.parseDouble(payment.getAmount()));
  }

  private SepTransactionStatus getSepDepositTransactionStatus(
      CustodyPaymentStatus custodyPaymentStatus) {
    switch (custodyPaymentStatus) {
      case SUCCESS:
        return SepTransactionStatus.COMPLETED;
      case ERROR:
        return SepTransactionStatus.ERROR;
      default:
        throw new RuntimeException(
            String.format("Unsupported custody transaction status[%s]", custodyPaymentStatus));
    }
  }

  private SepTransactionStatus getSepWithdrawalTransactionStatus(
      CustodyPaymentStatus custodyPaymentStatus) {
    switch (custodyPaymentStatus) {
      case SUCCESS:
        return SepTransactionStatus.PENDING_ANCHOR;
      case ERROR:
        return SepTransactionStatus.ERROR;
      default:
        throw new RuntimeException(
            String.format("Unsupported custody transaction status[%s]", custodyPaymentStatus));
    }
  }
}
