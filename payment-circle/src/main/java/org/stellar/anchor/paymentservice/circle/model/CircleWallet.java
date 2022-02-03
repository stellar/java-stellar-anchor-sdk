package org.stellar.anchor.paymentservice.circle.model;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;

@Data
public class CircleWallet {
  String walletId;
  String entityId;
  String type; // `merchant` or `end_user_wallet`
  String description;
  List<CircleBalance> balances;

  public Account.Capabilities getCapabilities() {
    Account.Capabilities capabilities = new Account.Capabilities(Network.CIRCLE, Network.STELLAR);
    capabilities.set(Network.BANK_WIRE, "merchant".equals(type));
    return capabilities;
  }

  public Account toAccount() {
    Account account = new Account(Network.CIRCLE, walletId, description, getCapabilities());
    account.setBalances(
        balances.stream().map(CircleBalance::toBalance).collect(Collectors.toList()));
    return account;
  }
}
