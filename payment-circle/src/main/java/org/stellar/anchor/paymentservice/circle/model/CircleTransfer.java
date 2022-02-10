package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter;
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
  Date createDate;
  Map<String, ?> originalResponse;

  public Payment toPayment(String distributionAccountId) {
    Payment p = new Payment();
    p.setId(id);
    p.setSourceAccount(source.toAccount(distributionAccountId));
    Account destinationAccount = destination.toAccount(distributionAccountId);
    p.setDestinationAccount(destinationAccount);
    p.setBalance(amount.toBalance(destinationAccount.network));
    p.setTxHash(transactionHash);
    p.setStatus(status.toPaymentStatus());
    p.setErrorCode(errorCode);
    p.setCreatedAt(createDate);
    p.setUpdatedAt(createDate);
    p.setOriginalResponse(originalResponse);

    return p;
  }

  public static class Deserializer implements JsonDeserializer<CircleTransfer> {
    @Override
    public CircleTransfer deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      Gson gson = new Gson();
      CircleTransfer transfer = gson.fromJson(jsonObject, CircleTransfer.class);

      Type type = new TypeToken<Map<String, ?>>() {}.getType();
      Map<String, Object> originalResponse = gson.fromJson(jsonObject, type);
      String createdDateStr = CircleDateFormatter.dateToString(transfer.getCreateDate());
      originalResponse.put("createDate", createdDateStr);
      transfer.setOriginalResponse(originalResponse);
      return transfer;
    }
  }
}
