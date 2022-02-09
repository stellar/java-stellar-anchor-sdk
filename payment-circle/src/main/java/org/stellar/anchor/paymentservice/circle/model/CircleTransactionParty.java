package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.annotations.SerializedName;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;

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

  public Account toAccount(String distributionAccountId) {
    switch (type) {
      case BLOCKCHAIN:
        if (!"XLM".equals(chain)) {
          throw new RuntimeException("the only supported chain is `XLM`");
        }
        return new Account(
            Network.STELLAR, address, addressTag, new Account.Capabilities(Network.STELLAR));

      case WALLET:
        Account account =
            new Account(
                Network.CIRCLE, id, new Account.Capabilities(Network.CIRCLE, Network.STELLAR));
        account.capabilities.set(Network.BANK_WIRE, distributionAccountId.equals(id));
        return account;

      case WIRE:
        return new Account(Network.BANK_WIRE, id, new Account.Capabilities(Network.BANK_WIRE));

      default:
        throw new RuntimeException("unsupported type");
        // TODO: make sure to handle cards and ach as well
    }
  }
}
