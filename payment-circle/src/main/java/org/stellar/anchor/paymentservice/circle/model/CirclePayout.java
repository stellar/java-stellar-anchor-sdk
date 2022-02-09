package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter;
import shadow.com.google.common.reflect.TypeToken;

@Data
public class CirclePayout {
  String id;
  String sourceWalletId;
  CircleTransactionParty destination;
  CircleBalance amount;
  CircleBalance fees;
  CirclePaymentStatus status;
  String trackingRef;
  String errorCode;
  Date updateDate;
  Date createDate;
  Map<String, String> riskEvaluation;
  Map<String, Map<String, String>> adjustments;

  @SerializedName("return")
  Map<String, ?> returnVal;

  public Payment toPayment() {
    Payment p = new Payment();
    p.setId(id);
    p.setSourceAccount(
        new Account(
            Network.CIRCLE,
            sourceWalletId,
            new Account.Capabilities(Network.CIRCLE, Network.STELLAR, Network.BANK_WIRE)));
    // In Circle, only the source wallet can send a Payout:
    Account destinationAccount = destination.toAccount(sourceWalletId);
    p.setDestinationAccount(destinationAccount);
    p.setBalance(amount.toBalance(destinationAccount.network));
    p.setStatus(status.toPaymentStatus());
    p.setErrorCode(errorCode);
    p.setCreatedAt(createDate);
    p.setUpdatedAt(updateDate);

    Gson gson = new Gson();
    String jsonString = gson.toJson(this);
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Map<String, Object> map = gson.fromJson(jsonString, type);
    map.put("createDate", CircleDateFormatter.dateToString(createDate));
    map.put("updateDate", CircleDateFormatter.dateToString(updateDate));
    p.setOriginalResponse(map);

    return p;
  }
}
