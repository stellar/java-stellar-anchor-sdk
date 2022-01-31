package org.stellar.anchor.paymentsservice;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Account {
    public String id;

    /**
     * A complementary identifier of the account. It might be considered mandatory depending on the use case.
     */
    public String idTag;

    public Network network;

    public List<Balance> balances = new ArrayList<>();

    /**
     * An object indicating which networks this account can interact with for sending and/or receiving funds.
     */
    public Capabilities capabilities;

    /**
     * The list of not-yet-available balances that are expected to settle shortly. These balances could be cancelled or
     * returned, in which cases they may never become available in the user account.
     */
    public List<Balance> unsettledBalances = new ArrayList<>();

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

        public Capabilities(List<Network> sendAndReceive) {
            this(sendAndReceive, sendAndReceive);
        }
    }
}
