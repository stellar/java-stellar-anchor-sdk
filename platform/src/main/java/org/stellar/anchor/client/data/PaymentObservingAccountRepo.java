package org.stellar.anchor.client.data;

import org.springframework.data.repository.CrudRepository;

public interface PaymentObservingAccountRepo
    extends CrudRepository<PaymentObservingAccount, String> {
  PaymentObservingAccount findByAccount(String account);
}
