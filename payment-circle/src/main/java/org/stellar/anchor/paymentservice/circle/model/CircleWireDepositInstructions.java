package org.stellar.anchor.paymentservice.circle.model;

import java.lang.reflect.Type;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.DepositInstructions;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import org.stellar.anchor.paymentservice.circle.util.CircleAsset;
import org.stellar.sdk.responses.GsonSingleton;
import shadow.com.google.common.reflect.TypeToken;
import shadow.com.google.gson.Gson;

@Data
public class CircleWireDepositInstructions {
  String trackingRef;
  Beneficiary beneficiary;
  BeneficiaryBank beneficiaryBank;

  @Data
  public static class Beneficiary {
    String name;
    String address1;
    String address2;
  }

  @Data
  public static class BeneficiaryBank {
    String name;
    String address;
    String city;
    String postalCode;
    String country;
    String swiftCode;
    String routingNumber;
    String accountNumber;
  }

  public DepositInstructions toDepositInstructions(String beneficiaryAccountId) {
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Gson gson = GsonSingleton.getInstance();
    Map<String, Object> originalResponse = gson.fromJson(gson.toJson(this), type);
    return new DepositInstructions(
        beneficiaryAccountId,
        null,
        PaymentNetwork.CIRCLE,
        trackingRef,
        null,
        PaymentNetwork.BANK_WIRE,
        CircleAsset.fiatUSD(),
        originalResponse);
  }
}
