package org.stellar.anchor.platform.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.sep31.RefundPayment;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdbcSep31RefundPayment implements RefundPayment {
  String id;
  String amount;
  String fee;
}
