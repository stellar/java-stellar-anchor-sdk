package org.stellar.anchor.horizon;

import lombok.Getter;
import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.Server;

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
}
