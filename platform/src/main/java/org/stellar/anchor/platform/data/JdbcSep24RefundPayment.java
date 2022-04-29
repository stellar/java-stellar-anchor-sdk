package org.stellar.anchor.platform.data;

import lombok.Data;
import org.stellar.anchor.sep24.Sep24RefundPayment;

@Data
public class JdbcSep24RefundPayment implements Sep24RefundPayment {
  String id;
  String idType;
  String amount;
  String fee;
}
