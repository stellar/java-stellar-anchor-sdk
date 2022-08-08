package org.stellar.anchor.platform.payment.observer.stellar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.stellar.anchor.platform.data.PaymentObservingAccount;
import org.stellar.anchor.platform.data.PaymentObservingAccountRepo;

public class PaymentObservingAccountStore {
  PaymentObservingAccountRepo repo;

  public PaymentObservingAccountStore(PaymentObservingAccountRepo repo) {
    this.repo = repo;
  }

  List<PaymentObservingAccount> list() {
    List<PaymentObservingAccount> result = new ArrayList<>();
    repo.findAll().forEach(result::add);
    return result;
  }

  void addOrUpdate(String account, Instant lastObserved) {
    PaymentObservingAccount poa = repo.findByAccount(account);
    if (poa == null) {
      poa = new PaymentObservingAccount(account, lastObserved);
      repo.save(poa);
    } else {
      poa.setLastObserved(lastObserved);
    }
  }

  void remove(String account) {
    PaymentObservingAccount poa = repo.findByAccount(account);
    if (poa != null) {
      repo.delete(poa);
    }
  }
}
