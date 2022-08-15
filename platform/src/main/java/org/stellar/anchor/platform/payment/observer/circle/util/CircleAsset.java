package org.stellar.anchor.platform.payment.observer.circle.util;

import java.util.List;
import org.stellar.sdk.Network;
import reactor.util.annotation.NonNull;

public class CircleAsset {
  @NonNull
  public static String stellarUSDC(Network stellarNetwork) {
    if (stellarNetwork == org.stellar.sdk.Network.PUBLIC)
      return "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN";

    return "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
  }

  public static String circleUSD() {
    return "circle:USD";
  }

  public static String fiatUSD() {
    return "iso4217:USD";
  }

  public static boolean isSupported(String currencyName, Network stellarNetwork) {
    return List.of(
            CircleAsset.circleUSD(), CircleAsset.fiatUSD(), CircleAsset.stellarUSDC(stellarNetwork))
        .contains(currencyName);
  }
}
