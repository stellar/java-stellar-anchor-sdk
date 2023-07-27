package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.api.sep.SepTransactionStatus.ERROR;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.warnF;
import static org.stellar.anchor.util.MathHelper.decimal;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.platform.config.RpcConfig;
import org.stellar.anchor.platform.custody.CustodyPayment.CustodyPaymentStatus;
import org.stellar.anchor.platform.data.CustodyTransactionStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

/**
 * Abstract custody payment handler. Contains common logic for payment validation, custody and SEP
 * transaction update
 */
public abstract class CustodyPaymentHandler {

  private static final Set<String> SUPPORTED_ASSET_TYPES =
      Set.of("credit_alphanum4", "credit_alphanum12", "native");

  private final JdbcCustodyTransactionRepo custodyTransactionRepo;
  private final PlatformApiClient platformApiClient;
  private final RpcConfig rpcConfig;

  public CustodyPaymentHandler(
      JdbcCustodyTransactionRepo custodyTransactionRepo,
      PlatformApiClient platformApiClient,
      RpcConfig rpcConfig) {
    this.custodyTransactionRepo = custodyTransactionRepo;
    this.platformApiClient = platformApiClient;
    this.rpcConfig = rpcConfig;
  }

  /**
   * Handle inbound(withdrawal) payment
   *
   * @param txn custody transaction
   * @param payment inbound custody payment
   * @throws AnchorException if error happens during SEP transaction update
   * @throws IOException if error happens during SEP transaction update
   */
  public abstract void onReceived(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException;

  /**
   * Handle outbound(deposit) payment
   *
   * @param txn custody transaction
   * @param payment outbound custody payment
   * @throws AnchorException if error happens during SEP transaction update
   * @throws IOException if error happens during SEP transaction update
   */
  public abstract void onSent(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException;

  protected void updateTransaction(
      JdbcCustodyTransaction txn, CustodyPayment payment, SepTransactionStatus newSepTxnStatus)
      throws AnchorException, IOException {
    txn.setUpdatedAt(payment.getCreatedAt());
    txn.setFromAccount(payment.getFrom());
    txn.setExternalTxId(payment.getExternalTxId());
    txn.setStatus(getCustodyTransactionStatus(payment.getStatus()).toString());
    custodyTransactionRepo.save(txn);

    if (ERROR == newSepTxnStatus) {
      platformApiClient.notifyTransactionError(
          txn.getId(), rpcConfig.getActions().getCustomMessages().getCustodyTransactionFailed());
    } else {
      switch (Kind.from(txn.getKind())) {
        case DEPOSIT:
          platformApiClient.notifyOnchainFundsSent(
              txn.getSepTxId(),
              payment.getTransactionHash(),
              rpcConfig.getActions().getCustomMessages().getOutgoingPaymentSent());
          break;
        case WITHDRAWAL:
        case RECEIVE:
          platformApiClient.notifyOnchainFundsReceived(
              txn.getSepTxId(),
              payment.getTransactionHash(),
              payment.getAmount(),
              rpcConfig.getActions().getCustomMessages().getIncomingPaymentReceived());
          break;
      }
    }
  }

  protected void validatePayment(JdbcCustodyTransaction txn, CustodyPayment payment) {
    if (CustodyPaymentStatus.SUCCESS != payment.getStatus()) {
      return;
    }

    if (!SUPPORTED_ASSET_TYPES.contains(payment.getAssetType())) {
      debugF(
          "Unsupported asset type[{}]. Payment: id[{}], externalTxId[{}]",
          payment.getAssetType(),
          payment.getId(),
          payment.getExternalTxId());
    }

    String paymentAssetName = "stellar:" + payment.getAssetName();
    if (!txn.getAmountAsset().equals(paymentAssetName)) {
      warnF(
          "Incoming payment asset[{}] does not match the expected asset[{}]. Payment: id[{}], "
              + "externalTxId[{}] ",
          payment.getAssetName(),
          txn.getAmountAsset(),
          payment.getId(),
          payment.getExternalTxId());
    }

    // Check if the payment contains the expected amount (or greater)
    BigDecimal expectedAmount = decimal(txn.getAmount());
    BigDecimal gotAmount = decimal(payment.getAmount());

    //    if (gotAmount.compareTo(expectedAmount) < 0) {
    //      warnF(
    //          "The incoming payment amount was insufficient. Expected: [{}], Received: [{}].
    // Payment: id[{}], externalTxId[{}]",
    //          formatAmount(expectedAmount),
    //          formatAmount(gotAmount),
    //          payment.getId(),
    //          payment.getExternalTxId());
    //    }
  }

  private CustodyTransactionStatus getCustodyTransactionStatus(
      CustodyPaymentStatus custodyPaymentStatus) {
    switch (custodyPaymentStatus) {
      case SUCCESS:
        return CustodyTransactionStatus.COMPLETED;
      case ERROR:
        return CustodyTransactionStatus.FAILED;
      default:
        throw new RuntimeException(
            String.format("Unsupported custody transaction status[%s]", custodyPaymentStatus));
    }
  }
}
