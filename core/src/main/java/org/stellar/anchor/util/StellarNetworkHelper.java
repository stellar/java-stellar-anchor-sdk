package org.stellar.anchor.util;

import org.stellar.sdk.Network;

public class StellarNetworkHelper {
  public static Network toStellarNetwork(String stellarNetworkPassphrase) {

    if (stellarNetworkPassphrase.equals(Network.TESTNET.getNetworkPassphrase())) {
      return Network.TESTNET;
    } else if (stellarNetworkPassphrase.equals(Network.PUBLIC.getNetworkPassphrase())) {
      return Network.PUBLIC;
    }

    throw new IllegalArgumentException(
        String.format(
            "Invalid Stellar network passphrase [%s] is specified.", stellarNetworkPassphrase));
  }
}
