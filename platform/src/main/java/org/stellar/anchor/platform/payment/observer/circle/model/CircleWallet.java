package org.stellar.anchor.platform.payment.observer.circle.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.DepositInstructions;
import org.stellar.anchor.platform.payment.common.PaymentNetwork;
import org.stellar.anchor.platform.payment.observer.circle.util.CircleAsset;

@Data
public class CircleWallet {
  String walletId;
  String entityId;
  String type; // `merchant` or `end_user_wallet`
  String description;
  List<CircleBalance> balances;

  public CircleWallet(String walletId) {
    this.walletId = walletId;
  }

  public Account.Capabilities getCapabilities() {
    return "merchant".equals(type) ? merchantAccountCapabilities() : defaultCapabilities();
  }

  public static Account.Capabilities defaultCapabilities() {
    Account.Capabilities capabilities =
        new Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR);
    capabilities.getSend().put(PaymentNetwork.BANK_WIRE, true);
    return capabilities;
  }

  public static Account.Capabilities merchantAccountCapabilities() {
    return new Account.Capabilities(
        PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR, PaymentNetwork.BANK_WIRE);
  }

  public Account toAccount() {
    Account account = new Account(PaymentNetwork.CIRCLE, walletId, description, getCapabilities());
    account.setBalances(
        balances.stream()
            .map(circleBalance -> circleBalance.toBalance(PaymentNetwork.CIRCLE))
            .collect(Collectors.toList()));
    account.setUnsettledBalances(new ArrayList<>());
    return account;
  }

  public DepositInstructions toDepositInstructions() {
    return new DepositInstructions(
        walletId,
        null,
        PaymentNetwork.CIRCLE,
        walletId,
        null,
        PaymentNetwork.CIRCLE,
        CircleAsset.circleUSD(),
        null);
  }
}
