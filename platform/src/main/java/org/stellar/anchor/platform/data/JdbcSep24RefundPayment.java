package org.stellar.anchor.platform.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;

@Getter
@Setter
@Entity
@Table(name = "sep24_refund_payment")
public class JdbcSep24RefundPayment implements Sep24RefundPayment {
  @Id
  @GeneratedValue
  @Column(name = "sep24_refund_payment_id")
  UUID jdbcId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id", nullable = false)
  JdbcSep24Transaction transaction;

  String id;
  String idType;
  String amount;
  String fee;
}
