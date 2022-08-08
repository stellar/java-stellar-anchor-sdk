package org.stellar.anchor.platform.data;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stellar_payment_observing_account")
public class PaymentObservingAccount {
  public PaymentObservingAccount() {}

  public PaymentObservingAccount(String account, Instant lastObserved) {
    this.account = account;
    this.lastObserved = lastObserved;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false)
  private Long id;

  String account;
  Instant lastObserved;
}
