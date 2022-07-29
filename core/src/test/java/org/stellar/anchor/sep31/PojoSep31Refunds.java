package org.stellar.anchor.sep31;

import java.util.List;
import lombok.Data;

@Data
public class PojoSep31Refunds implements Refunds {
  String amountRefunded;
  String amountFee;
  List<RefundPayment> refundPayments;
}
