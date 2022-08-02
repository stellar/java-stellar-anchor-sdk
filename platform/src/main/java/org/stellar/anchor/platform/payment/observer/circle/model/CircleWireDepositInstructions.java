package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.platform.payment.common.DepositInstructions;
import org.stellar.anchor.platform.payment.common.PaymentNetwork;
import org.stellar.anchor.platform.payment.observer.circle.util.CircleAsset;
import org.stellar.anchor.util.GsonUtils;
import shadow.com.google.common.reflect.TypeToken;

@Data
public class CircleWireDepositInstructions {
  String trackingRef;
  Beneficiary beneficiary;
  BeneficiaryBank beneficiaryBank;

  private static final Gson gson = GsonUtils.getInstance();

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
