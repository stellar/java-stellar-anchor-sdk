package org.stellar.anchor.platform.payment.observer.stellar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.stellar.anchor.platform.data.PaymentObservingAccount;
import org.stellar.anchor.platform.data.PaymentObservingAccountRepo;

public class PaymentObservingAccountStore {
  PaymentObservingAccountRepo repo;

  List<PaymentObservingAccount> list() {
    List<PaymentObservingAccount> result = new ArrayList<>();
    repo.findAll().forEach(result::add);
    return result;
  }

  void add(String account, Instant startAt) {
    PaymentObservingAccount poa = repo.findByAccount(account);
    if (poa == null) {
      poa = new PaymentObservingAccount(account, startAt);
      repo.save(poa);
    }
  }

  void remove(String account) {
    PaymentObservingAccount poa = repo.findByAccount(account);
    if (poa != null) {
      repo.delete(poa);
    }
  }
}
