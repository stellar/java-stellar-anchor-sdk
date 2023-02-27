package org.stellar.anchor.platform.data;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@NoArgsConstructor
@Table(name = "stellar_payment_observing_account")
@TypeDef(name = "json", typeClass = JsonType.class)
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
