package org.stellar.anchor.util;

import org.stellar.sdk.Network;

public class StellarNetworkHelper {
  public static Network toStellarNetwork(String stellarNetwork) {
    switch (stellarNetwork) {
      case "PUBLIC":
      case "public:":
        return Network.PUBLIC;
      case "TESTNET":
      case "testnet":
        return Network.TESTNET;
      default:
        throw new IllegalArgumentException(
            String.format("Invalid Stellar [%s] network is specified.", stellarNetwork));
    }
  }
}
