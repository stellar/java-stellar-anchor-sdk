package org.stellar.anchor.api.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StellarPayment {
  String id;

  Amount amount;

  @SerializedName("payment_type")
  Type paymentType;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  public enum Type {
    @SerializedName("payment")
    PAYMENT("payment"),

    @SerializedName("path_payment")
    PATH_PAYMENT("path_payment");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
