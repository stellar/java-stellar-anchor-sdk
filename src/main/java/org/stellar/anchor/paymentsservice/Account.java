package org.stellar.anchor.paymentsservice;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Account {
    public String id;

    /**
     * A complementary identifier of the account. It might be considered mandatory depending on the use case.
     */
    public String idTag;

    public Network network;

    public AccountLevel level;

    public List<Balance> balances = new ArrayList<>();

    /**
     * The list of not-yet-available balances that are expected to settle shortly. These balances could be cancelled or
     * returned, in which cases they may never become available in the user account.
     */
    public List<Balance> unsettledBalances = new ArrayList<>();
}
