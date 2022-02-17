package org.stellar.anchor.paymentservice.circle.util;

import org.stellar.sdk.Network;
import reactor.util.annotation.NonNull;

public class CircleAsset {
  @NonNull
  public static String stellarUSDC(Network stellarNetwork) {
    if (stellarNetwork == org.stellar.sdk.Network.PUBLIC)
      return "USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN";

    return "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
  }
}
