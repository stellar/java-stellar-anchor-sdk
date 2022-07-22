package org.stellar.anchor.sep31;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.sep31.Sep31Transaction.RefundPayment;

@Data
public class PojoSep31Refunds implements Sep31Transaction.Refunds {
  String amountRefunded;
  String amountFee;
  List<RefundPayment> refundPayments;
}
