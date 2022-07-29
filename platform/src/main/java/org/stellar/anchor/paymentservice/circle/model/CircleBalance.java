package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.util.CircleAsset;
import paymentservice.Balance;
import paymentservice.PaymentNetwork;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class CircleBalance {
  @NonNull String amount;
  @NonNull String currency;
  @Nullable org.stellar.sdk.Network stellarNetwork;

  public CircleBalance(
      @NonNull String currency,
      @NonNull String amount,
      @Nullable org.stellar.sdk.Network stellarNetwork) {
    this.currency = currency;
    this.amount = amount;
    this.stellarNetwork = stellarNetwork;
  }

  public CircleBalance(@NonNull String currency, @NonNull String amount) {
    this(currency, amount, null);
  }

  public Balance toBalance(@NonNull PaymentNetwork destinationPaymentNetwork) {
    String currencyName = destinationPaymentNetwork.getCurrencyPrefix() + ":" + currency;
    if (currencyName.equals("stellar:USD")) currencyName = stellarUSDC();

    return new Balance(amount, currencyName);
  }

  @NonNull
  public String stellarUSDC() {
    return CircleAsset.stellarUSDC(stellarNetwork);
  }
}
