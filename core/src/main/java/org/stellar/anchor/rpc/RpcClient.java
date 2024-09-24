package org.stellar.anchor.rpc;

import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.responses.sorobanrpc.SimulateTransactionResponse;

public class RpcClient {
  private final SorobanServer sorobanServer;

  public RpcClient(AppConfig appConfig) {
    this.sorobanServer = new SorobanServer(appConfig.getRpcServerUrl());
  }

  public SorobanServer getServer() {
    return this.sorobanServer;
  }

  public TransactionBuilderAccount getAccount(String accountId) {
    try {
      return this.sorobanServer.getAccount(accountId);
    } catch (Exception e) {
      throw new RuntimeException("Error getting account: " + e.getMessage());
    }
  }

  public SimulateTransactionResponse simulateTransaction(Transaction transaction) {
    try {
      return this.sorobanServer.simulateTransaction(transaction);
    } catch (Exception e) {
      throw new RuntimeException("Error simulating transaction: " + e.getMessage());
    }
  }
}
