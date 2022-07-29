package org.stellar.anchor.sep31;

import lombok.Data;

@Data
public class PojoSep31RefundPayment implements RefundPayment {
  String id;
  String amount;
  String fee;
}
