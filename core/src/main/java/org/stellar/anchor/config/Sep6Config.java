package org.stellar.anchor.config;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface Sep6Config {
  boolean isEnabled();

  Transactions getTransactions();

  @AllArgsConstructor
  @Getter
  class Transactions {
    @SerializedName("enabled")
    boolean enabled;
  }

  Transaction getTransaction();

  @AllArgsConstructor
  @Getter
  class Transaction {
    @SerializedName("enabled")
    boolean enabled;
  }

  Features getFeatures();

  @AllArgsConstructor
  @Getter
  class Features {
    @SerializedName("account_creation")
    boolean accountCreation;

    @SerializedName("claimable_balances")
    boolean claimableBalances;
  }
}
