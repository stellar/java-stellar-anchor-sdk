package org.stellar.anchor.rpc;

import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.SorobanServer;

public class RpcClient {
  private final SorobanServer sorobanServer;

  public RpcClient(AppConfig appConfig) {
    this.sorobanServer = new SorobanServer(appConfig.getRpcServerUrl());
  }

  public SorobanServer getServer() {
    return this.sorobanServer;
  }
}
