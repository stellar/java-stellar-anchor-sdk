package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.traceF;
import static org.stellar.anchor.util.Log.warnF;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;
import static org.stellar.anchor.util.StringHelper.isNotEmpty;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.platform.custody.CustodyPayment.CustodyPaymentStatus;
import org.stellar.anchor.platform.data.CustodyTransactionStatus;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

public abstract class CustodyPaymentHandler {

  private static final Set<String> SUPPORTED_ASSET_TYPES =
      Set.of("credit_alphanum4", "credit_alphanum12");

  private final JdbcCustodyTransactionRepo custodyTransactionRepo;
  private final PlatformApiClient platformApiClient;

  public CustodyPaymentHandler(
      JdbcCustodyTransactionRepo custodyTransactionRepo, PlatformApiClient platformApiClient) {
    this.custodyTransactionRepo = custodyTransactionRepo;
    this.platformApiClient = platformApiClient;
  }

  public abstract void onReceived(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException;

  public abstract void onSent(JdbcCustodyTransaction txn, CustodyPayment payment)
      throws AnchorException, IOException;

  protected void updateTransaction(
      JdbcCustodyTransaction txn, CustodyPayment payment, SepTransactionStatus newSepTxnStatus)
      throws AnchorException, IOException {
    txn.setUpdatedAt(payment.getCreatedAt());
    txn.setStatus(getCustodyTransactionStatus(payment.getStatus()).toString());
    custodyTransactionRepo.save(txn);

    patchTransaction(txn, payment, newSepTxnStatus);
  }

  protected void validatePayment(JdbcCustodyTransaction txn, CustodyPayment payment) {
    if (!SUPPORTED_ASSET_TYPES.contains(payment.getAssetType())) {
      debugF(
          "Unsupported asset type[{}]. Payment: id[{}], externalTxId[{}]",
          payment.getAssetType(),
          payment.getId(),
          payment.getExternalTxId());
      payment.setStatus(CustodyPaymentStatus.ERROR);
      payment.setMessage("Unsupported asset type");
      return;
    }

    String paymentAssetName = "stellar:" + payment.getAssetName();
    if (!txn.getAmountInAsset().equals(paymentAssetName)) {
      warnF(
          "Incoming payment asset[{}] does not match the expected asset[{}]. Payment: id[{}], "
              + "externalTxId[{}] ",
          payment.getAssetName(),
          txn.getAmountInAsset(),
          payment.getId(),
          payment.getExternalTxId());
      payment.setStatus(CustodyPaymentStatus.ERROR);
      payment.setMessage("Incoming asset does not match the expected asset");
      return;
    }

    // Check if the payment contains the expected amount (or greater)
    BigDecimal expectedAmount = decimal(txn.getAmountIn());
    BigDecimal gotAmount = decimal(payment.getAmount());

    if (gotAmount.compareTo(expectedAmount) < 0) {
      warnF(
          "The incoming payment amount was insufficient. Expected: [{}], Received: [{}]. Payment: id[{}], externalTxId[{}]",
          formatAmount(expectedAmount),
          formatAmount(gotAmount),
          payment.getId(),
          payment.getExternalTxId());
    }
  }

  private void patchTransaction(
      JdbcCustodyTransaction txn,
      CustodyPayment payment,
      SepTransactionStatus newSepTransactionStatus)
      throws IOException, AnchorException {
    List<StellarTransaction> stellarTransactions = new ArrayList<>();

    if (isNotEmpty(payment.getId())) {
      debugF(
          "Building StellarTransaction. Payment: id[{}], externalTxId[{}]",
          payment.getId(),
          payment.getExternalTxId());

      StellarTransaction stellarTransaction =
          StellarTransaction.builder()
              .id(payment.getTransactionHash())
              .memo(txn.getMemo())
              .memoType(txn.getMemoType())
              .createdAt(payment.getCreatedAt())
              .envelope(payment.getTransactionEnvelope())
              .payments(
                  List.of(
                      StellarPayment.builder()
                          .id(payment.getId())
                          .paymentType(
                              CustodyPayment.Type.PAYMENT == payment.getType()
                                  ? StellarPayment.Type.PAYMENT
                                  : StellarPayment.Type.PATH_PAYMENT)
                          .sourceAccount(payment.getFrom())
                          .destinationAccount(payment.getTo())
                          .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                          .build()))
              .build();
      stellarTransactions.add(stellarTransaction);
    }

    PatchTransactionsRequest patchTransactionsRequest =
        PatchTransactionsRequest.builder()
            .records(
                List.of(
                    PatchTransactionRequest.builder()
                        .transaction(
                            PlatformTransactionData.builder()
                                .id(txn.getId())
                                .updatedAt(payment.getCreatedAt())
                                .transferReceivedAt(payment.getCreatedAt())
                                .status(newSepTransactionStatus)
                                .stellarTransactions(stellarTransactions)
                                .build())
                        .build()))
            .build();

    debugF("Patching transaction {}.", txn.getId());
    traceF("Patching transaction {} with request {}.", txn.getId(), patchTransactionsRequest);

    platformApiClient.patchTransaction(patchTransactionsRequest);
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
