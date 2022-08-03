package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.Payment;
import org.stellar.anchor.platform.payment.common.PaymentNetwork;
import org.stellar.anchor.util.GsonUtils;
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
  Instant updateDate;
  Instant createDate;
  Map<String, String> riskEvaluation;
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
    private static final Gson gson = GsonUtils.getInstance();

    @Override
    public CirclePayout deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      CirclePayout payout = gson.fromJson(jsonObject, CirclePayout.class);

      Type type = new TypeToken<Map<String, ?>>() {}.getType();
      Map<String, Object> originalResponse = gson.fromJson(jsonObject, type);
      payout.setOriginalResponse(originalResponse);
      return payout;
    }
  }
}
