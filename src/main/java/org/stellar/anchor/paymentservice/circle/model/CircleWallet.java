package org.stellar.anchor.paymentservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;

import java.util.ArrayList;
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
        List<Network> sendOrReceiveCapabilities = new ArrayList<>(List.of(Network.CIRCLE, Network.STELLAR));
        if ("merchant".equals(type)) {
            sendOrReceiveCapabilities.add(Network.BANK_WIRE);
        }
        return new Account.Capabilities(sendOrReceiveCapabilities);
    }

    public Account toAccount() {
        Account account = new Account();
        account.setId(walletId);
        account.setIdTag(description);
        account.setNetwork(Network.CIRCLE);
        account.setBalances(balances.stream().map(CircleBalance::toBalance).collect(Collectors.toList()));
        account.setCapabilities(getCapabilities());
        return account;
    }
}
