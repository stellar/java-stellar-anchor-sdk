package org.stellar.anchor.platform.data;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.sep24.Sep24Refunds;

@Data
public class JdbcSep24Refunds implements Sep24Refunds {
  String amountRefunded;
  String amountFee;
  List<JdbcSep24RefundPayment> payments;
}
