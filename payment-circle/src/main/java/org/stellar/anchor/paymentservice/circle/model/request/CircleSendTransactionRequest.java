package org.stellar.anchor.paymentservice.circle.model.request;

import java.util.HashMap;
import java.util.List;
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
      CircleTransactionParty source,
      CircleTransactionParty destination,
      CircleBalance amount,
      String idempotencyKey) {
    CircleSendTransactionRequest req = new CircleSendTransactionRequest();
    req.source = source;
    req.destination = destination;
    if (List.of(
            "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
            "USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN")
        .contains(amount.getCurrency())) {
      amount.setCurrency("USD");
    }
    req.amount = amount;
    req.idempotencyKey = idempotencyKey;
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
