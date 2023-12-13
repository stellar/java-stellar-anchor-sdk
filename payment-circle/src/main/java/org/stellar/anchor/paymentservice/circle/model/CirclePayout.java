package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter;

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
  Map<String, ?> originalResponse;

  @SerializedName("return")
  Map<String, ?> returnVal;

  public Payment toPayment() {
    Payment p = new Payment();
    p.setId(id);
    p.setSourceAccount(
        new Account(
            PaymentNetwork.CIRCLE,
            sourceWalletId,
            new Account.Capabilities(
                PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR, PaymentNetwork.BANK_WIRE)));
    Account destinationAccount = destination.toAccount(null);
    p.setDestinationAccount(destinationAccount);
    p.setBalance(amount.toBalance(destinationAccount.paymentNetwork));
    p.setStatus(status.toPaymentStatus());
    p.setErrorCode(errorCode);
    p.setCreatedAt(createDate);
    p.setUpdatedAt(updateDate);
    p.setOriginalResponse(originalResponse);

    return p;
  }

  public static class Deserializer implements JsonDeserializer<CirclePayout> {
    @Override
    public CirclePayout deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      Gson gson = new Gson();
      CirclePayout payout = gson.fromJson(jsonObject, CirclePayout.class);

      Type type = new TypeToken<Map<String, ?>>() {}.getType();
      Map<String, Object> originalResponse = gson.fromJson(jsonObject, type);
      String createDateStr = CircleDateFormatter.dateToString(payout.getCreateDate());
      originalResponse.put("createDate", createDateStr);
      String updateDateStr = CircleDateFormatter.dateToString(payout.getUpdateDate());
      originalResponse.put("updateDate", updateDateStr);
      payout.setOriginalResponse(originalResponse);
      return payout;
    }
  }
}
