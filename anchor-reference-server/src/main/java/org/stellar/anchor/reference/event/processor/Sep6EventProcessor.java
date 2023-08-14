package org.stellar.anchor.reference.event.processor;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.stellar.anchor.api.callback.GetCustomerRequest;
import org.stellar.anchor.api.callback.GetCustomerResponse;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.event.ActiveTransactionStore;
import org.stellar.anchor.reference.service.CustomerService;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@AllArgsConstructor
public class Sep6EventProcessor implements SepAnchorEventProcessor {
  private final AppSettings appSettings;
  private final PlatformApiClient platformApiClient;
  private final Server server;
  private final CustomerService customerService;
  private final ActiveTransactionStore activeTransactionStore;

  @Override
  public void onQuoteCreatedEvent(AnchorEvent event) {
    // TODO: Implement for exchange flows
  }

  @Override
  public void onTransactionCreated(AnchorEvent event) {
    PlatformTransactionData.Kind kind = event.getTransaction().getKind();
    Log.infoF("Received transaction created event: {}", event);
    switch (kind) {
      case DEPOSIT:
        onDepositTransactionCreated(event);
        break;
      case WITHDRAWAL:
        throw new NotImplementedException("Withdrawals not implemented yet");
      default:
        Log.warnF("Unexpected transaction kind: {}", kind);
        break;
    }
  }

  @Override
  public void onTransactionError(AnchorEvent event) {
    Log.infoF("Received transaction error event: {}", event);
  }

  private void onDepositTransactionCreated(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();

    if (event.getTransaction().getStatus() == SepTransactionStatus.INCOMPLETE) {
      SepTransactionStatus newStatus = SepTransactionStatus.PENDING_ANCHOR;

      patchTransaction(
          PlatformTransactionData.builder()
              .id(event.getTransaction().getId())
              .status(newStatus)
              .build());
      try {
        GetCustomerResponse customer =
            customerService.getCustomer(
                GetCustomerRequest.builder()
                    .account(event.getTransaction().getDestinationAccount())
                    .build());
        activeTransactionStore.add(customer.getId(), event.getTransaction().getId());
        Log.infoF(
            "Added transaction {} to active transaction store for customer {}",
            event.getTransaction().getId(),
            customer.getId());
      } catch (NotFoundException e) {
        Log.error(
            "Error getting customer for account {}",
            event.getTransaction().getDestinationAccount());
      }
    } else {
      Log.warnF("Unexpected transaction status: {}", eventStatus);
    }
  }

  @Override
  public void onTransactionStatusChanged(AnchorEvent event) {
    PlatformTransactionData.Kind kind = event.getTransaction().getKind();
    Log.infoF("Received transaction status changed event: {}", event);
    switch (kind) {
      case DEPOSIT:
        onDepositTransactionStatusChanged(event);
        break;
      case WITHDRAWAL:
        throw new NotImplementedException("Withdrawals not implemented yet");
      default:
        Log.warnF("Unexpected transaction kind: {}", kind);
        break;
    }
  }

  private void onDepositTransactionStatusChanged(AnchorEvent event) {
    GetTransactionResponse transaction = event.getTransaction();
    SepTransactionStatus eventStatus = transaction.getStatus();
    switch (transaction.getStatus()) {
      case PENDING_ANCHOR:
        patchTransaction(
            PlatformTransactionData.builder()
                .id(transaction.getId())
                .updatedAt(Instant.now())
                .status(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE)
                .build());
        break;
      case COMPLETED:
        try {
          GetCustomerResponse customer =
              customerService.getCustomer(
                  GetCustomerRequest.builder().id(transaction.getDestinationAccount()).build());
          activeTransactionStore.remove(customer.getId(), event.getTransaction().getId());
          Log.infoF(
              "Removed transaction {} from active transaction store for customer {}",
              event.getTransaction().getId(),
              customer.getId());
        } catch (NotFoundException e) {
          Log.error("Error getting customer for account {}", transaction.getDestinationAccount());
        }
      default:
        Log.warnF("Unexpected transaction status: {}", eventStatus);
        break;
    }
  }

  @Override
  public void onCustomerUpdated(AnchorEvent event) {
    Log.infoF("Received KYC updated event: {}", event);
    String observedAccount = event.getCustomer().getId();
    Set<String> transactionIds = activeTransactionStore.getTransactions(observedAccount);
    Log.infoF(
        "Found {} transactions to update for account {}", transactionIds.size(), observedAccount);

    for (String id : transactionIds) {
      try {
        GetTransactionResponse transaction = platformApiClient.getTransaction(id);

        if (transaction.getStatus() == SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE) {
          KeyPair keyPair = KeyPair.fromSecretSeed(appSettings.getSecret());
          String assetCode = toStandardFormat(transaction.getAmountOut().getAsset());

          Asset asset = Asset.create(assetCode);
          String amount = transaction.getAmountOut().getAmount();
          String destination = transaction.getDestinationAccount();

          StellarTransaction stellarTxn =
              submitStellarTransaction(keyPair.getAccountId(), destination, asset, amount);

          patchTransaction(
              PlatformTransactionData.builder()
                  .id(transaction.getId())
                  .status(SepTransactionStatus.COMPLETED)
                  .updatedAt(Instant.now())
                  .completedAt(Instant.now())
                  .requiredInfoMessage(null)
                  .requiredInfoUpdates(null)
                  // TODO: format to be decided in ANCHOR-386
                  .how("Placeholder deposit instructions")
                  .stellarTransactions(List.of(stellarTxn))
                  .build());
        }
      } catch (AnchorException e) {
        Log.error("Error getting transaction", e);
      } catch (IOException e) {
        Log.error("Error submitting transaction", e);
      }
    }
  }

  private String toStandardFormat(String asset) {
    String[] assetCodeParts = asset.split(":");
    if (assetCodeParts.length == 3) {
      return assetCodeParts[1] + ":" + assetCodeParts[2];
    } else if (assetCodeParts.length == 2) {
      return assetCodeParts[1];
    } else {
      throw new RuntimeException("invalid asset code");
    }
  }

  private StellarTransaction submitStellarTransaction(
      String source, String destination, Asset asset, String amount) {
    try {
      AccountResponse account = server.accounts().account(source);
      Transaction transaction =
          new TransactionBuilder(account, Network.TESTNET)
              .setBaseFee(100)
              .setTimeout(60)
              .addOperation(new PaymentOperation.Builder(destination, asset, amount).build())
              .build();
      transaction.sign(KeyPair.fromSecretSeed(appSettings.getSecret()));
      SubmitTransactionResponse txnResp = server.submitTransaction(transaction);

      if (!txnResp.isSuccess()) {
        throw new RuntimeException(
            "failed to send transaction with error: "
                + txnResp.getExtras().getResultCodes().getTransactionResultCode());
      }

      String txHash = txnResp.getHash();

      String operationId =
          server.operations().forTransaction(txHash).execute().getRecords().stream()
              .filter(operation -> operation instanceof PaymentOperationResponse)
              .findFirst()
              .map(OperationResponse::getId)
              .map(Object::toString)
              .orElse(null);

      StellarPayment stellarPayment =
          new StellarPayment(
              operationId,
              new Amount(amount, asset.toString()),
              StellarPayment.Type.PAYMENT,
              source,
              destination);

      return StellarTransaction.builder().id(txHash).payments(List.of(stellarPayment)).build();
    } catch (IOException | AccountRequiresMemoException e) {
      throw new RuntimeException("Failed to submit transaction", e);
    }
  }

  private void patchTransaction(PlatformTransactionData data) {
    PatchTransactionsRequest request =
        PatchTransactionsRequest.builder()
            .records(List.of(PatchTransactionRequest.builder().transaction(data).build()))
            .build();

    try {
      platformApiClient.patchTransaction(request);
    } catch (IOException | AnchorException e) {
      throw new RuntimeException("Failed to update transaction", e);
    }
  }
}
