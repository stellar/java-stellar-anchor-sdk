package org.stellar.anchor.paymentsservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentsservice.Balance;
import org.stellar.anchor.paymentsservice.Network;

@Data
public class CircleBalance {
    String amount;
    String currency;

    public Balance toBalance() {
        return new Balance(amount, Network.CIRCLE.getCurrencyPrefix() + ":" + currency);
    }
}
