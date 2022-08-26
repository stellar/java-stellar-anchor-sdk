package org.stellar.anchor.api.shared;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
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
public class StellarTransaction {
  String id;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  Instant createdAt;

  String envelope;

  List<StellarPayment> payments;

  public static List<StellarTransaction> addOrUpdateTransactions(
      List<StellarTransaction> transactionList, StellarTransaction... newTransactions) {
    HashMap<String, StellarTransaction> txHashMap = new HashMap<>();

    if (transactionList != null) {
      for (StellarTransaction tx : transactionList) {
        txHashMap.put(tx.getId(), tx);
      }
    }

    if (newTransactions != null) {
      for (StellarTransaction tx : newTransactions) {
        List<StellarPayment> newPayments = tx.getPayments();
        if (txHashMap.containsKey(tx.getId())) {
          StellarTransaction existingTx = txHashMap.get(tx.getId());
          newPayments =
              StellarPayment.addOrUpdatePayments(
                  existingTx.getPayments(), tx.getPayments().toArray(StellarPayment[]::new));
        }
        tx.setPayments(newPayments);

        txHashMap.put(tx.getId(), tx);
      }
    }

    return new ArrayList<>(txHashMap.values());
  }
}
