package org.stellar.anchor.horizon;

import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.operations.OperationResponse;

/** The horizon-server. */
public class Horizon {
  @Getter private final String horizonUrl;
  @Getter private final String stellarNetworkPassphrase;
  private final Server horizonServer;

  public Horizon(AppConfig appConfig) {
    this.horizonUrl = appConfig.getHorizonUrl();
    this.stellarNetworkPassphrase = appConfig.getStellarNetworkPassphrase();
    this.horizonServer = new Server(appConfig.getHorizonUrl());
  }

  public Server getServer() {
    return this.horizonServer;
  }

  public List<OperationResponse> getStellarTxnOperations(String stellarTxnId) throws IOException {
    return getServer()
        .payments()
        .includeTransactions(true)
        .forTransaction(stellarTxnId)
        .execute()
        .getRecords();
  }
}
