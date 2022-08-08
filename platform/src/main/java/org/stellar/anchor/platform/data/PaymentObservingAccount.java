package org.stellar.anchor.platform.data;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "stellar_payment_observing_account")
public class PaymentObservingAccount {
  public PaymentObservingAccount() {}

  public PaymentObservingAccount(String account, Instant startAt) {
    this.account = account;
    this.startAt = startAt;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false)
  private Long id;

  String account;
  Instant startAt;
}
