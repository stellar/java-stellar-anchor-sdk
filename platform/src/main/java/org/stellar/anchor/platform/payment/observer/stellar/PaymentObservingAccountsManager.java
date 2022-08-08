package org.stellar.anchor.platform.payment.observer.stellar;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.stellar.anchor.platform.data.PaymentObservingAccount;

public class PaymentObservingAccountsManager {
  Map<String, ObservingAccount> allAccounts;
  private PaymentObservingAccountStore store;

  public PaymentObservingAccountsManager(PaymentObservingAccountStore store) {
    this.store = store;
    allAccounts = new ConcurrentHashMap<>();
  }

  @PostConstruct
  void initialize() {
    List<PaymentObservingAccount> accounts = store.list();
    for (PaymentObservingAccount account : accounts) {
      ObservingAccount oa =
          new ObservingAccount(account.getAccount(), account.getLastObserved(), true);
      upsert(oa);
    }
  }

  /**
   * Adds an account to be observed. If the account is being observed, it will be updated.
   *
   * @param account The account being observed.
   * @param expiring true if the account can be expired. false, otherwise.
   */
  public void upsert(String account, Boolean expiring) {
    if (account != null && expiring != null) {
      upsert(new ObservingAccount(account, Instant.now(), expiring));
    }
  }

  /**
   * Add an account to be observed. If the account is being observed, it will be updated.
   *
   * @param observingAccount The account being observed.
   */
  public void upsert(ObservingAccount observingAccount) {
    if (observingAccount != null) {
      allAccounts.put(observingAccount.account, observingAccount);
    }
  }

  /**
   * Remove the account from the observed account list.
   *
   * @param account The account being removed.
   */
  public void remove(String account) {
    if (account != null) {
      ObservingAccount targetAccount = allAccounts.get(account);
      if (targetAccount != null) {
        allAccounts.remove(account);
      }
    }
  }

  /**
   * Gets the list of observed accounts.
   *
   * @return The list of observed accounts.
   */
  public List<ObservingAccount> getAccounts() {
    return new ArrayList<>(allAccounts.values());
  }

  /**
   * Check if the account is being observed. If the account is being observed, the lastObserved
   * timestamp of the observing account will be updated.
   *
   * @param account The account to be checked.
   * @return true if the account is being observed. false, otherwise.
   */
  public boolean observe(String account) {
    ObservingAccount acct = allAccounts.get(account);
    if (acct == null) return false;
    acct.lastObserved = Instant.now();
    return true;
  }

  /**
   * Evict expired accounts
   *
   * @param maxAge evict all accounts that are older than maxAge
   */
  public void evict(Duration maxAge) {
    for (ObservingAccount acct : getAccounts()) {
      if (!acct.expiring) continue;

      Duration age = Duration.between(Instant.now(), acct.lastObserved).abs();
      if (age.compareTo(maxAge) > 0) {
        allAccounts.remove(acct.account);
      }
    }
  }

  @AllArgsConstructor
  public static class ObservingAccount {
    String account;
    Instant lastObserved;

    Boolean expiring;
  }
}
