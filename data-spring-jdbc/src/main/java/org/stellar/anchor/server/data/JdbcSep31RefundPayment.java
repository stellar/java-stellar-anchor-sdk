package org.stellar.anchor.server.data;

import lombok.Data;
import org.stellar.anchor.model.Sep31Transaction;

public class JdbcSep31RefundPayment {
  @Data
  public static class JdbcRefundPayment implements Sep31Transaction.RefundPayment {
    String Id;
    String amount;
    String fee;
  }
}
