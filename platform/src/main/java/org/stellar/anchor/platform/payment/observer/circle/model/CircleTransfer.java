package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.Payment;
import org.stellar.anchor.util.GsonUtils;
import shadow.com.google.common.reflect.TypeToken;

@Data
public class CircleTransfer {
  String id;
  CircleTransactionParty source;
  CircleTransactionParty destination;
  CircleBalance amount;
  String transactionHash;
  CirclePaymentStatus status;
  String errorCode;
  Instant createDate;
  Map<String, ?> originalResponse;

  public Payment toPayment(String distributionAccountId) {
    Payment p = new Payment();
    p.setId(id);
    p.setSourceAccount(source.toAccount(distributionAccountId));
    Account destinationAccount = destination.toAccount(distributionAccountId);
    p.setDestinationAccount(destinationAccount);
    p.setBalance(amount.toBalance(destinationAccount.paymentNetwork));
    p.setIdTag(transactionHash);
    p.setStatus(status.toPaymentStatus());
    p.setErrorCode(errorCode);
    p.setCreatedAt(createDate);
    p.setUpdatedAt(createDate);
    p.setOriginalResponse(originalResponse);

    return p;
  }

  public static class Serialization
      implements JsonDeserializer<CircleTransfer>, JsonSerializer<CircleTransfer> {
    private static final Gson gson = GsonUtils.getInstance();

    @Override
    public CircleTransfer deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      CircleTransfer transfer = gson.fromJson(jsonObject, CircleTransfer.class);

      Type type = new TypeToken<Map<String, ?>>() {}.getType();
      Map<String, Object> originalResponse = gson.fromJson(jsonObject, type);
      transfer.setOriginalResponse(originalResponse);

      return transfer;
    }

    @Override
    public JsonElement serialize(
        CircleTransfer src, Type typeOfSrc, JsonSerializationContext context) {
      return gson.toJsonTree(src.originalResponse).getAsJsonObject();
    }
  }
}
