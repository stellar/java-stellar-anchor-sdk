package org.stellar.anchor.sep6;

import lombok.Data;

@Data
public class PojoSep6RefundPayment implements Sep6RefundPayment {
  String id;
  String idType;
  String amount;
  String fee;
}
