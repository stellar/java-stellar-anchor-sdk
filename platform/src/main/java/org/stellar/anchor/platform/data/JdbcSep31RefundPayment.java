package org.stellar.anchor.platform.data;

import lombok.Data;
import org.stellar.anchor.sep31.RefundPayment;

public class JdbcSep31RefundPayment {
  @Data
  public static class JdbcRefundPayment implements RefundPayment {
    String Id;
    String amount;
    String fee;
  }
}
