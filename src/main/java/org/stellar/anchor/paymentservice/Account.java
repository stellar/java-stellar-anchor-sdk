package org.stellar.anchor.paymentservice;

import lombok.Data;
import lombok.Getter;
import reactor.util.annotation.Nullable;
import reactor.util.annotation.NonNull;

import java.util.*;

@Data
public class Account {
    @NonNull
    public Network network;

    @NonNull
    public String id;

    /**
     * A complementary identifier of the account. It might be considered mandatory depending on the use case.
     */
    @Nullable
    public String idTag;

    /**
     * An object indicating which networks this account can interact with for sending and/or receiving funds.
     */
    @NonNull
    public Account.Capabilities capabilities;

    /**
     * The list of not-yet-available balances that are expected to settle shortly. These balances could be cancelled or
     * returned, in which cases they may never become available in the user account.
     */
    @NonNull
    public List<Balance> unsettledBalances = new ArrayList<>();

    @NonNull
    public List<Balance> balances = new ArrayList<>();

    public Account(@NonNull Network network, @NonNull String id, @Nullable String idTag, @NonNull Account.Capabilities capabilities) {
        this.network = network;
        this.id = id;
        this.idTag = idTag;
        this.capabilities = capabilities;
    }

    public Account(@NonNull Network network, @NonNull String id, @NonNull Account.Capabilities capabilities) {
        this.id = id;
        this.network = network;
        this.capabilities = capabilities;
    }

    @Getter
    public static class Capabilities {
        private final Map<Network, Boolean> send = new HashMap<>();

        private final Map<Network, Boolean> receive = new HashMap<>();

        /**
         * Capabilities is an object indicating which networks this account can interact with for sending and/or
         * receiving funds.
         *
         * @param send    contains a list of networks this account can send funds to.
         * @param receive contains a list of networks this account can receive funds from.
         */
        public Capabilities(List<Network> send, List<Network> receive) {
            for (Network network : Network.values()) {
                this.send.put(network, false);
                this.receive.put(network, false);
            }

            for (Network network : send) {
                this.send.put(network, true);
            }

            for (Network network : receive) {
                this.receive.put(network, true);
            }
        }

        public Capabilities(Network... sendAndReceive) {
            this(List.of(sendAndReceive), List.of(sendAndReceive));
        }

        public void setSendSupport(Network network, Boolean supportEnabled) {
            this.send.put(network, supportEnabled);
        }

        public void setReceiveSupport(Network network, Boolean supportEnabled) {
            this.receive.put(network, supportEnabled);
        }

        public void setFullSupport(Network network, Boolean supportEnabled) {
            this.send.put(network, supportEnabled);
            this.receive.put(network, supportEnabled);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Capabilities that = (Capabilities) o;
            return this.send.keySet().equals(that.send.keySet()) && this.receive.keySet().equals(that.receive.keySet());
        }
    }
}
