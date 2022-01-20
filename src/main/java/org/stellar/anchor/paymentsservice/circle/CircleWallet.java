package org.stellar.anchor.paymentsservice.circle;

import lombok.Data;
import org.stellar.anchor.paymentsservice.Account;
import org.stellar.anchor.paymentsservice.AccountType;
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

    public AccountType getAccountType() {
        if ("merchant".equals(type)) {
            return AccountType.DISTRIBUTION;
        }
        return AccountType.DEFAULT;
    }

    public Account toAccount() {
        Account account = new Account();
        account.setId(walletId);
        account.setIdTag(description);
        account.setNetwork(Network.CIRCLE);
        account.setBalances(balances.stream().map(CircleBalance::toBalance).collect(Collectors.toList()));
        account.setType(getAccountType());
        return account;
    }
}
