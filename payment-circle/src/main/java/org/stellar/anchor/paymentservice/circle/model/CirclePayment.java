package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter;
import shadow.com.google.common.reflect.TypeToken;

@Data
public class CirclePayment {
  String id;
  String type; // payment
  String merchantId;
  String merchantWalletId;
  CircleTransactionParty source;
  String description;
  CircleBalance amount;
  CircleBalance fees;
  CirclePaymentStatus status;
  List<Map<String, ?>> refunds;
  Date updateDate;
  Date createDate;

  Map<String, String> riskEvaluation;
  String trackingRef;
  String errorCode;
  Map<String, ?> cancel;
  Map<String, ?> metadata;
  Verification verification;

  Map<String, ?> originalResponse;

  @Data
  public static class Verification {
    String avs;
    String cvv;
  }

  public Payment toPayment() {
    Payment p = new Payment();
    p.setId(id);
    p.setSourceAccount(source.toAccount(null));
    p.setDestinationAccount(
        new Account(
            PaymentNetwork.CIRCLE,
            merchantWalletId,
            new Account.Capabilities(
                PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR, PaymentNetwork.BANK_WIRE)));
    p.setBalance(amount.toBalance(PaymentNetwork.CIRCLE));
    p.setStatus(status.toPaymentStatus());
    p.setErrorCode(errorCode);
    p.setCreatedAt(createDate);
    p.setUpdatedAt(updateDate);
    p.setOriginalResponse(originalResponse);

    return p;
  }

  public static class Deserializer implements JsonDeserializer<CirclePayment> {
    @Override
    public CirclePayment deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      Gson gson = new Gson();
      CirclePayment payment = gson.fromJson(jsonObject, CirclePayment.class);

      Type type = new TypeToken<Map<String, ?>>() {}.getType();
      Map<String, Object> originalResponse = gson.fromJson(jsonObject, type);
      String createDateStr = CircleDateFormatter.dateToString(payment.getCreateDate());
      originalResponse.put("createDate", createDateStr);
      String updateDateStr = CircleDateFormatter.dateToString(payment.getUpdateDate());
      originalResponse.put("updateDate", updateDateStr);
      payment.setOriginalResponse(originalResponse);
      return payment;
    }
  }
}
