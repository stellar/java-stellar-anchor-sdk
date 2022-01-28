package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.Gson;
import lombok.Data;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter;
import shadow.com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

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

    public Payment toPayment() {
        Payment p = new Payment();
        p.setId(id);
        p.setSourceAccount(source.toAccount());
        p.setDestinationAccount(destination.toAccount());
        p.setBalance(amount.toBalance());
        p.setStatus(status.toPaymentStatus());
        p.setErrorCode(errorCode);
        p.setCreatedAt(createDate);
        p.setUpdatedAt(createDate);

        Gson gson = new Gson();
        String jsonString = gson.toJson(this);
        Type type = new TypeToken<Map<String, ?>>(){}.getType();
        Map<String, Object> map = gson.fromJson(jsonString, type);
        String createdDateStr = CircleDateFormatter.dateToString(createDate);
        map.put("createDate", createdDateStr);
        p.setOriginalResponse(map);

        return p;
    }
}
