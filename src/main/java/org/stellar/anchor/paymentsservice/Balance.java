package org.stellar.anchor.paymentsservice;

import lombok.Data;

@Data
public class Balance {
    String amount;
    String currencyName;

    public Balance(String amount, String currencyName) {
        this.amount = amount;
        this.currencyName = currencyName;
    }
}
