package org.stellar.anchor.api.shared;

import lombok.Data;

@Data
public class StellarPayment {
  String id;
  String paymentType;
  String sourceAccount;
  String destinationAccount;
  Amount amount;
}
