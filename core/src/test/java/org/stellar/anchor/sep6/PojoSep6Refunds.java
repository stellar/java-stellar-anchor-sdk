package org.stellar.anchor.sep6;

import java.util.List;
import lombok.Data;

@Data
public class PojoSep6Refunds implements Sep6Refunds {
  String amountRefunded;
  String amountFee;
  List<Sep6RefundPayment> payments;
}
