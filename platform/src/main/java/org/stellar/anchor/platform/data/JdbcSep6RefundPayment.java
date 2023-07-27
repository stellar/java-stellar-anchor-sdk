package org.stellar.anchor.platform.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.stellar.anchor.sep6.Sep6RefundPayment;

@Getter
@Setter
@NoArgsConstructor
public class JdbcSep6RefundPayment implements Sep6RefundPayment {
  String id;
  String idType;
  String amount;
  String fee;
}
