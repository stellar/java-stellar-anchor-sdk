package org.stellar.anchor.api.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StellarPayment {
  String id;

  Amount amount;

  @JsonProperty("payment_type")
  @SerializedName("payment_type")
  Type paymentType;

  @JsonProperty("source_account")
  @SerializedName("source_account")
  String sourceAccount;

  @JsonProperty("destination_account")
  @SerializedName("destination_account")
  String destinationAccount;

  public enum Type {
    @JsonProperty("payment")
    @SerializedName("payment")
    PAYMENT("payment"),

    @JsonProperty("path_payment")
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

  public static List<StellarPayment> addOrUpdatePayments(
      List<StellarPayment> paymentList, StellarPayment... newPayments) {
    HashMap<String, StellarPayment> paymentHashMap = new HashMap<>();

    if (paymentList != null) {
      for (StellarPayment p : paymentList) {
        paymentHashMap.put(p.getId(), p);
      }
    }

    if (newPayments != null) {
      for (StellarPayment p : newPayments) {
        paymentHashMap.put(p.getId(), p);
      }
    }

    return new ArrayList<>(paymentHashMap.values());
  }
}
