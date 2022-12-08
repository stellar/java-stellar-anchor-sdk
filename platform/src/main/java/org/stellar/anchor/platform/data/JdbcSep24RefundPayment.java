package org.stellar.anchor.platform.data;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;

@Getter
@Setter
@Entity
@Table(name = "sep24_refund_payment")
public class JdbcSep24RefundPayment implements Sep24RefundPayment {
  String id;
  String amount;
  String fee;
}
