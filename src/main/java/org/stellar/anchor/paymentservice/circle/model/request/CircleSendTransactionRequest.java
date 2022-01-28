package org.stellar.anchor.paymentservice.circle.model.request;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBalance;
import org.stellar.anchor.paymentservice.circle.model.CircleTransactionParty;

import java.util.HashMap;

@Data
public class CircleSendTransactionRequest {
    CircleTransactionParty source;
    CircleTransactionParty destination;
    CircleBalance amount;
    String idempotencyKey;
    HashMap<String, Object> metadata;

    public static CircleSendTransactionRequest forTransfer(CircleTransactionParty source, CircleTransactionParty destination, CircleBalance amount) {
        CircleSendTransactionRequest req = new CircleSendTransactionRequest();
        req.source = source;
        req.destination = destination;
        req.amount = amount;
        return req;
    }
}
