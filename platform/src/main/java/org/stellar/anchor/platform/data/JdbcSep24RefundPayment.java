package org.stellar.anchor.platform.data;

import lombok.Data;
import org.stellar.anchor.sep24.Sep24RefundPayment;

import javax.persistence.*;

@Data
@Entity
@Table(name = "sep24_refund_payment")
public class JdbcSep24RefundPayment implements Sep24RefundPayment {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "sep24_refund_payment_id")
  Long jdbcId;

  @ManyToOne
  @JoinColumn(name = "jdbc_id", nullable = false)
  private JdbcSep24Transaction transaction;

  String id;
  String idType;
  String amount;
  String fee;
}
