package org.stellar.anchor.platform.data;

import lombok.Data;
import org.stellar.anchor.sep31.RefundPayment;

@Data
public class JdbcSep31RefundPayment implements RefundPayment {
  String id;
  String amount;
  String fee;
}
