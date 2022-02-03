package org.stellar.anchor.paymentservice.circle.model.request;

import java.util.HashMap;
import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBalance;
import org.stellar.anchor.paymentservice.circle.model.CircleTransactionParty;

@Data
public class CircleSendTransactionRequest {
  CircleTransactionParty source;
  CircleTransactionParty destination;
  CircleBalance amount;
  String idempotencyKey;
  HashMap<String, Object> metadata;

  public static CircleSendTransactionRequest forTransfer(
      CircleTransactionParty source, CircleTransactionParty destination, CircleBalance amount) {
    CircleSendTransactionRequest req = new CircleSendTransactionRequest();
    req.source = source;
    req.destination = destination;
    req.amount = amount;
    return req;
  }

  public static CircleSendTransactionRequest forPayout(
      CircleTransactionParty source,
      CircleTransactionParty destination,
      CircleBalance amount,
      String idempotencyKey) {
    CircleSendTransactionRequest req = new CircleSendTransactionRequest();
    req.source = source;
    req.destination = destination;
    req.amount = amount;
    req.idempotencyKey = idempotencyKey;
    req.metadata = new HashMap<>();
    req.metadata.put("beneficiaryEmail", destination.getEmail());
    return req;
  }
}
