package org.stellar.anchor.reference.event;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.reference.client.PlatformApiClient;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.util.Log;
import org.stellar.platform.apis.platform.requests.PatchTransactionRequest;
import org.stellar.platform.apis.platform.requests.PatchTransactionsRequest;
import org.stellar.platform.apis.shared.Amount;

import java.io.IOException;
import java.util.List;

@Component
public class AnchorEventProcessor {
  private final PlatformApiClient platformClient;

  AnchorEventProcessor(AppSettings appSettings, Gson gson) {
    this.platformClient = new PlatformApiClient(appSettings.getPlatformApiEndpoint());
  }

  public void handleQuoteEvent(QuoteEvent event) {
    Log.debug(String.format("Received quote event: %s", event));
  }

  public void handleTransactionEvent(TransactionEvent event) {
    Log.debug(String.format("Received transaction event: %s", event));
    // NOTE: this code skips processing the received payment and just marks the
    // transaction as complete.
    PatchTransactionsRequest txnRequest =
        PatchTransactionsRequest.builder()
            .records(
                List.of(
                    PatchTransactionRequest.builder()
                        .id(event.getId())
                        .status(TransactionEvent.Status.COMPLETED.status)
                        .amountFee(
                            new Amount(
                                event.getAmountFee().getAmount(),
                                event.getAmountFee().getAsset()))
                        .amountOut(
                            new Amount(
                                event.getAmountOut().getAmount(), 
                                event.getAmountOut().getAsset()))
                        .build()))
            .build();
    try {
      platformClient.patchTransaction(txnRequest);
    } catch (IOException | AnchorException ex) {
      Log.errorEx(ex);
    }
  }
}
