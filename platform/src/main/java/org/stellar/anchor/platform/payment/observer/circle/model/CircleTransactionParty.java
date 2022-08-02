package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.annotations.SerializedName;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.PaymentNetwork;
import reactor.util.annotation.Nullable;

@lombok.Data
public class CircleTransactionParty {
  Type type;
  String id;
  String address;
  String addressTag;
  String name;
  transient String email;
  String chain;

  public enum Type {
    @SerializedName("wallet")
    WALLET("wallet"),

    @SerializedName("blockchain")
    BLOCKCHAIN("blockchain"),

    @SerializedName("wire")
    WIRE("wire"),

    @SerializedName("ach")
    ACH("ach"),

    @SerializedName("card")
    CARD("card");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static CircleTransactionParty wallet(String id) {
    CircleTransactionParty party = new CircleTransactionParty();
    party.type = Type.WALLET;
    party.id = id;
    return party;
  }

  public static CircleTransactionParty stellar(String address, String addressTag) {
    CircleTransactionParty party = new CircleTransactionParty();
    party.type = Type.BLOCKCHAIN;
    party.address = address;
    party.addressTag = addressTag;
    party.chain = "XLM";
    return party;
  }

  public static CircleTransactionParty wire(String bankId, String email) {
    CircleTransactionParty party = new CircleTransactionParty();
    party.type = Type.WIRE;
    party.id = bankId;
    party.email = email;
    return party;
  }

  /**
   * Transforms a Circle transaction party into an Account.
   *
   * @param distributionAccountId used to update the bank wire capability when this is a Circle
   *     wallet.
   * @return a new account instance.
   */
  public Account toAccount(@Nullable String distributionAccountId) {
    switch (type) {
      case BLOCKCHAIN:
        if (!"XLM".equals(chain)) {
          throw new RuntimeException("the only supported chain is `XLM`");
        }
        return new Account(
            PaymentNetwork.STELLAR,
            address,
            addressTag,
            new Account.Capabilities(PaymentNetwork.STELLAR));

      case WALLET:
        boolean isMerchantAccount =
            distributionAccountId != null && distributionAccountId.equals(id);
        Account.Capabilities capabilities =
            isMerchantAccount
                ? CircleWallet.merchantAccountCapabilities()
                : CircleWallet.defaultCapabilities();
        return new Account(PaymentNetwork.CIRCLE, id, capabilities);

      case WIRE:
        return new Account(
            PaymentNetwork.BANK_WIRE, id, new Account.Capabilities(PaymentNetwork.BANK_WIRE));

      default:
        throw new RuntimeException("unsupported type");
        // TODO: make sure to handle cards and ach as well
    }
  }
}
