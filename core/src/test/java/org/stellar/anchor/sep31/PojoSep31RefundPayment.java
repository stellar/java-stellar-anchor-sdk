package org.stellar.anchor.sep31;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PojoSep31RefundPayment implements RefundPayment {
  String id;
  String amount;
  String fee;
}
