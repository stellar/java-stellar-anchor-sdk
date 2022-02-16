package org.stellar.anchor.paymentservice.circle.model;

import java.util.Date;
import lombok.Data;
import reactor.util.annotation.NonNull;

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
}
