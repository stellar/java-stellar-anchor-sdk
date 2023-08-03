package org.stellar.anchor.reference.event;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
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
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@AllArgsConstructor
public class Sep6EventProcessor implements IAnchorEventProcessor {
  private final AppSettings appSettings;
  private final PlatformApiClient platformApiClient;
  private final Server server;
  private final ActiveTransactionStore activeTransctionStore;

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
        handleDepositTransactionCreated(event);
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
    // TODO: Implement
  }

  private void handleDepositTransactionCreated(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();

    if (event.getTransaction().getStatus() == SepTransactionStatus.INCOMPLETE) {
      SepTransactionStatus newStatus = SepTransactionStatus.PENDING_ANCHOR;

      PatchTransactionsRequest txnRequest =
          PatchTransactionsRequest.builder()
              .records(
                  List.of(
                      PatchTransactionRequest.builder()
                          .transaction(
                              PlatformTransactionData.builder()
                                  .id(event.getTransaction().getId())
                                  .status(newStatus)
                                  .build())
                          .build()))
              .build();
      try {
        platformApiClient.patchTransaction(txnRequest);
        activeTransctionStore.add(
            event.getTransaction().getDestinationAccount(), event.getTransaction().getId());
      } catch (IOException | AnchorException ex) {
        Log.errorEx(ex);
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
        handleDepositTransactionStatusChanged(event);
        break;
      case WITHDRAWAL:
        throw new NotImplementedException("Withdrawals not implemented yet");
      default:
        Log.warnF("Unexpected transaction kind: {}", kind);
        break;
    }
  }

  private void handleDepositTransactionStatusChanged(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();
    switch (event.getTransaction().getStatus()) {
      case PENDING_ANCHOR:
        PatchTransactionsRequest txnRequest =
            PatchTransactionsRequest.builder()
                .records(
                    List.of(
                        PatchTransactionRequest.builder()
                            .transaction(
                                PlatformTransactionData.builder()
                                    .id(event.getTransaction().getId())
                                    .status(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE)
                                    .requiredInfoMessage(
                                        "Provide the missing information to continue with the transaction.")
                                    .requiredInfoUpdates("TODO: implement this")
                                    .build())
                            .build()))
                .build();
        try {
          platformApiClient.patchTransaction(txnRequest);
        } catch (IOException | AnchorException ex) {
          Log.errorEx(ex);
        }
        break;
      case COMPLETED:
        activeTransctionStore.remove(
            event.getTransaction().getDestinationAccount(), event.getTransaction().getId());
      default:
        Log.warnF("Unexpected transaction status: {}", eventStatus);
        break;
    }
  }

  @Override
  public void onKycUpdatedEvent(AnchorEvent event) {
    Log.infoF("Received KYC updated event: {}", event);
    String observedAccount = event.getCustomer().getAccount();
    Set<String> transactionIds =
        activeTransctionStore.getTransactionIdsByStellarAccount(observedAccount);

    Log.infoF("Found {} transactions to update", transactionIds.size());

    for (String id : transactionIds) {
      try {
        GetTransactionResponse response = platformApiClient.getTransaction(id);

        if (response.getStatus() == SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE) {
          try {
            KeyPair keyPair = KeyPair.fromSecretSeed(appSettings.getSecret());
            AccountResponse account = server.accounts().account(keyPair.getAccountId());
            String[] assetCodeParts = response.getAmountOut().getAsset().split(":");
            String assetCode;
            if (assetCodeParts.length == 3) {
              assetCode = assetCodeParts[1] + ":" + assetCodeParts[2];
            } else if (assetCodeParts.length == 2) {
              assetCode = assetCodeParts[1];
            } else {
              throw new RuntimeException("invalid asset code");
            }

            Asset asset = Asset.create(assetCode);
            String amount = response.getAmountOut().getAmount();
            String destination = response.getDestinationAccount();

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
                    new Amount(amount, assetCode),
                    StellarPayment.Type.PAYMENT,
                    keyPair.getAccountId(),
                    destination);

            PatchTransactionsRequest txnRequest =
                PatchTransactionsRequest.builder()
                    .records(
                        List.of(
                            PatchTransactionRequest.builder()
                                .transaction(
                                    PlatformTransactionData.builder()
                                        .id(response.getId())
                                        .status(SepTransactionStatus.COMPLETED)
                                        .requiredInfoMessage(null)
                                        .requiredInfoUpdates(null)
                                        .how("Placeholder deposit instructions")
                                        .stellarTransactions(
                                            List.of(
                                                StellarTransaction.builder()
                                                    .id(txHash)
                                                    .payments(List.of(stellarPayment))
                                                    .build()))
                                        .build())
                                .build()))
                    .build();
            try {
              platformApiClient.patchTransaction(txnRequest);
            } catch (IOException | AnchorException ex) {
              Log.errorEx(ex);
            }
          } catch (Exception e) {
            throw new RuntimeException("failed to submit transaction", e);
          }
        }
      } catch (IOException | AnchorException e) {
        Log.error("Error getting transaction", e);
      }
    }
  }
}
