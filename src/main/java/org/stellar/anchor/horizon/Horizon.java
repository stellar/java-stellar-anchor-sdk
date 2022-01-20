package org.stellar.anchor.horizon;

import org.stellar.anchor.config.AppConfig;
import org.stellar.sdk.Server;

/**
 * The horizon-server.
 */
public class Horizon {
    final Server horizonServer;

    public Horizon(AppConfig appConfig) {
        horizonServer = new Server(appConfig.getHorizonURI());
    }

    public Server getServer() {
        return this.horizonServer;
    }
}
