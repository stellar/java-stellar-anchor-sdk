package org.stellar.anchor.platform.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;

@Getter
@Setter
@Entity
@Table(name = "sep24_refund_payment")
public class JdbcSep24RefundPayment implements Sep24RefundPayment {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "sep24_refund_payment_id")
  Long jdbcId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id", nullable = false)
  JdbcSep24Transaction transaction;

  String id;
  String idType;
  String amount;
  String fee;
}
