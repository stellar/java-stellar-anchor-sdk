package org.stellar.anchor.paymentsservice.circle.model;

import lombok.Data;
import org.stellar.anchor.paymentsservice.Account;
import org.stellar.anchor.paymentsservice.AccountLevel;
import org.stellar.anchor.paymentsservice.Network;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CircleWallet {
    String walletId;
    String entityId;
    String type;    // `merchant` or `end_user_wallet`
    String description;
    List<CircleBalance> balances;

    public AccountLevel getAccountLevel() {
        if ("merchant".equals(type)) {
            return AccountLevel.DISTRIBUTION;
        }
        return AccountLevel.DEFAULT;
    }

    public Account toAccount() {
        Account account = new Account();
        account.setId(walletId);
        account.setIdTag(description);
        account.setNetwork(Network.CIRCLE);
        account.setBalances(balances.stream().map(CircleBalance::toBalance).collect(Collectors.toList()));
        account.setLevel(getAccountLevel());
        return account;
    }
}
