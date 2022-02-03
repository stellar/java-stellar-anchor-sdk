package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.Balance;
import org.stellar.anchor.paymentservice.Network;

@Data
public class CircleBalance {
  String amount;
  String currency;

  public CircleBalance(String currency, String amount) {
    this.currency = currency;
    this.amount = amount;
  }

  public Balance toBalance() {
    return new Balance(amount, Network.CIRCLE.getCurrencyPrefix() + ":" + currency);
  }
}
