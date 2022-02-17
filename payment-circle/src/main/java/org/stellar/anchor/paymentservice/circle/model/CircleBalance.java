package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.Balance;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import reactor.util.annotation.NonNull;

@Data
public class CircleBalance {
  @NonNull String amount;
  @NonNull String currency;
  @NonNull org.stellar.sdk.Network stellarNetwork;

  public CircleBalance(
      @NonNull String currency,
      @NonNull String amount,
      @NonNull org.stellar.sdk.Network stellarNetwork) {
    this.currency = currency;
    this.amount = amount;
    this.stellarNetwork = stellarNetwork;
  }

  public Balance toBalance(@NonNull PaymentNetwork destinationPaymentNetwork) {
    String currencyName = destinationPaymentNetwork.getCurrencyPrefix() + ":" + currency;
    if (currencyName.equals("stellar:USD"))
      currencyName = destinationPaymentNetwork.getCurrencyPrefix() + ":" + stellarUSDC();

    return new Balance(amount, currencyName);
  }

  @NonNull
  public String stellarUSDC() {
    if (stellarNetwork == org.stellar.sdk.Network.PUBLIC)
      return "USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN";

    return "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
  }
}
