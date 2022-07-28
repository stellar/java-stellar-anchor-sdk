package org.stellar.anchor.sep31;

import lombok.Data;

@Data
public class PojoSep31RefundPayment implements Sep31Transaction.RefundPayment {
  String id;
  String amount;
  String fee;
}
