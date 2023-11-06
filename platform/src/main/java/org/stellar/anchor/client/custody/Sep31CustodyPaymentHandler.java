package org.stellar.anchor.client.custody;

import static org.stellar.anchor.client.data.CustodyTransactionStatus.FAILED;
import static org.stellar.anchor.util.Log.infoF;
import static org.stellar.anchor.util.Log.warn;

import java.io.IOException;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.client.config.RpcConfig;
import org.stellar.anchor.client.data.CustodyTransactionStatus;
import org.stellar.anchor.client.data.JdbcCustodyTransaction;
import org.stellar.anchor.client.data.JdbcCustodyTransaction.PaymentType;
import org.stellar.anchor.client.data.JdbcCustodyTransactionRepo;
import org.stellar.anchor.client.service.AnchorMetrics;
import org.stellar.anchor.metrics.MetricsService;

/** Custody payment handler for SEP31 transactions */
public class Sep31CustodyPaymentHandler extends CustodyPaymentHandler {

  private final PlatformApiClient platformApiClient;
  private final RpcConfig rpcConfig;
  private final MetricsService metricsService;

  public Sep31CustodyPaymentHandler(
      JdbcCustodyTransactionRepo custodyTransactionRepo,
      PlatformApiClient platformApiClient,
      RpcConfig rpcConfig,
      MetricsService metricsService) {
    super(custodyTransactionRepo);
    this.platformApiClient = platformApiClient;
    this.rpcConfig = rpcConfig;
    this.metricsService = metricsService;
  }

  @Override
  public void onReceived(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException {
    infoF(
        "Incoming inbound payment for SEP-31 transaction. Payment: id[{}], externalTxId[{}], type[{}]",
        payment.getId(),
        payment.getExternalTxId(),
        txn.getType());

    validatePayment(txn, payment);
    updateTransaction(txn, payment);

    if (FAILED == CustodyTransactionStatus.from(txn.getStatus())) {
      platformApiClient.notifyTransactionError(
          txn.getSepTxId(), rpcConfig.getCustomMessages().getCustodyTransactionFailed());
    } else {
      switch (PaymentType.from(txn.getType())) {
        case PAYMENT:
          platformApiClient.notifyOnchainFundsReceived(
              txn.getSepTxId(),
              payment.getTransactionHash(),
              payment.getAmount(),
              rpcConfig.getCustomMessages().getIncomingPaymentReceived());

          metricsService
              .counter(AnchorMetrics.PAYMENT_RECEIVED, "asset", payment.getAssetName())
              .increment(Double.parseDouble(payment.getAmount()));

          break;
        case REFUND:
          platformApiClient.notifyRefundSent(
              txn.getSepTxId(),
              payment.getTransactionHash(),
              payment.getAmount(),
              txn.getAmountFee(),
              txn.getAsset());
          break;
      }
    }
  }

  @Override
  public void onSent(JdbcCustodyTransaction tx, CustodyPayment payment) {
    warn("Outbound payments are not implemented for SEP31");
  }
}
