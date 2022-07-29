package org.stellar.anchor.platform.payment.common;

import java.util.*;
import lombok.Data;
import lombok.Getter;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Data
public class Account {
  @NonNull public PaymentNetwork paymentNetwork;

  @NonNull public String id;

  /**
   * A complementary identifier of the account. It might be considered mandatory depending on the
   * use case.
   */
  @Nullable public String idTag;

  /**
   * An object indicating which networks this account can interact with for sending and/or receiving
   * funds.
   */
  @NonNull public Account.Capabilities capabilities;

  @Nullable public List<Balance> balances;

  /**
   * The list of not-yet-available balances that are expected to settle shortly. These balances
   * could be cancelled or returned, in which cases they may never become available in the user
   * account.
   */
  @Nullable public List<Balance> unsettledBalances;

  public Account(
      @NonNull PaymentNetwork paymentNetwork,
      @NonNull String id,
      @Nullable String idTag,
      @NonNull Account.Capabilities capabilities) {
    this.paymentNetwork = paymentNetwork;
    this.id = id;
    this.idTag = idTag;
    this.capabilities = capabilities;
  }

  public Account(
      @NonNull PaymentNetwork paymentNetwork,
      @NonNull String id,
      @NonNull Account.Capabilities capabilities) {
    this.id = id;
    this.paymentNetwork = paymentNetwork;
    this.capabilities = capabilities;
  }

  @Getter
  public static class Capabilities {
    private final Map<PaymentNetwork, Boolean> send = new HashMap<>();

    private final Map<PaymentNetwork, Boolean> receive = new HashMap<>();

    /**
     * Capabilities is an object indicating which networks this account can interact with for
     * sending and/or receiving funds.
     *
     * @param send contains a list of networks this account can send funds to.
     * @param receive contains a list of networks this account can receive funds from.
     */
    public Capabilities(List<PaymentNetwork> send, List<PaymentNetwork> receive) {
      for (PaymentNetwork paymentNetwork : PaymentNetwork.values()) {
        this.send.put(paymentNetwork, false);
        this.receive.put(paymentNetwork, false);
      }

      if (send != null) {
        for (PaymentNetwork paymentNetwork : send) {
          this.send.put(paymentNetwork, true);
        }
      }

      if (receive != null) {
        for (PaymentNetwork paymentNetwork : receive) {
          this.receive.put(paymentNetwork, true);
        }
      }
    }

    public Capabilities(PaymentNetwork... sendAndReceive) {
      this(List.of(sendAndReceive), List.of(sendAndReceive));
    }

    public void set(PaymentNetwork paymentNetwork, Boolean supportEnabled) {
      this.send.put(paymentNetwork, supportEnabled);
      this.receive.put(paymentNetwork, supportEnabled);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Capabilities that = (Capabilities) o;
      return this.send.equals(that.send) && this.receive.equals(that.receive);
    }

    @Override
    public String toString() {
      return "Capabilities{" + "send=" + send + ", receive=" + receive + '}';
    }
  }
}
