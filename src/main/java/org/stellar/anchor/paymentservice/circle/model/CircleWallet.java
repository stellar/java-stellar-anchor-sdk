package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CircleWallet {
    String walletId;
    String entityId;
    String type;    // `merchant` or `end_user_wallet`
    String description;
    List<CircleBalance> balances;

    public Account.Capabilities getCapabilities() {
        return new Account.Capabilities(Network.CIRCLE, Network.STELLAR);
    }

    public Account toAccount() {
        Account account = new Account(Network.CIRCLE, walletId, description, getCapabilities());
        account.setBalances(balances.stream().map(CircleBalance::toBalance).collect(Collectors.toList()));
        return account;
    }
}
