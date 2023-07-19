package org.stellar.anchor.config;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface Sep6Config {
  boolean isEnabled();

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
