package org.stellar.anchor.config;

import com.google.gson.annotations.SerializedName;

public interface Sep6Config {
  boolean isEnabled();

  Transactions getTransactions();

  class Transactions {
    @SerializedName("enabled")
    Boolean enabled;

    @SerializedName("authentication_required")
    Boolean authenticationRequired;
  }

  Transaction getTransaction();

  class Transaction {
    @SerializedName("enabled")
    Boolean enabled;

    @SerializedName("authentication_required")
    Boolean authenticationRequired;
  }

  Features getFeatures();

  class Features {
    @SerializedName("account_creation")
    Boolean accountCreation;

    @SerializedName("claimable_balances")
    Boolean claimableBalances;
  }
}
