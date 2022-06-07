package org.stellar.anchor.sep24;

import lombok.Data;

@Data
public class PojoSep24RefundPayment implements Sep24RefundPayment {
  String id;
  String idType;
  String amount;
  String fee;
}
