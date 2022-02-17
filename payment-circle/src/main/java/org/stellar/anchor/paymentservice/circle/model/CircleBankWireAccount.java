package org.stellar.anchor.paymentservice.circle.model;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.DepositInstructions;
import org.stellar.anchor.paymentservice.PaymentNetwork;
import org.stellar.sdk.responses.GsonSingleton;
import reactor.util.annotation.NonNull;
import shadow.com.google.common.reflect.TypeToken;
import shadow.com.google.gson.Gson;

@Data
public class CircleBankWireAccount {
  @NonNull String status;
  @NonNull String id;
  @NonNull String trackingRef;
  @NonNull String fingerprint;
  @NonNull String description;
  @NonNull BillingDetails billingDetails;
  @NonNull BankAddress bankAddress;
  @NonNull Date createDate;
  @NonNull Date updateDate;

  @Data
  public static class BillingDetails extends Address {
    String name;
  }

  @Data
  public static class BankAddress extends Address {
    String bankName;
  }

  @Data
  static class Address {
    String line1;
    String line2;
    String city;
    String postalCode;
    String district;
    String country;
  }

  public DepositInstructions toDepositInstructions(String beneficiaryAccountId) {
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Gson gson = GsonSingleton.getInstance();
    Map<String, Object> originalResponse = gson.fromJson(gson.toJson(this), type);
    return new DepositInstructions(
        beneficiaryAccountId,
        null,
        PaymentNetwork.CIRCLE,
        id,
        trackingRef,
        PaymentNetwork.BANK_WIRE,
        "iso4217:USD",
        originalResponse);
  }
}
