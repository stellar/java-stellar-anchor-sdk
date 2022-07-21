package org.stellar.anchor.sep31;

import lombok.Data;
import org.stellar.anchor.sep31.Sep31Transaction.RefundPayment;

import java.util.List;

@Data
public class PojoSep31Refunds implements Sep31Transaction.Refunds {
  String amountRefunded;
  String amountFee;
  List<RefundPayment> refundPayments;
}
