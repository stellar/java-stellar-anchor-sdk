package org.stellar.anchor.platform.data;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@NoArgsConstructor
@Table(name = "stellar_payment_observing_account")
public class PaymentObservingAccount {
  public PaymentObservingAccount(String account, Instant lastObserved) {
    this.account = account;
    this.lastObserved = lastObserved;
  }

  @Column(unique = true)
  @Id
  String account;

  @Column(name = "last_observed")
  Instant lastObserved;
}
