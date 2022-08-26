package org.stellar.anchor.api.shared;

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
