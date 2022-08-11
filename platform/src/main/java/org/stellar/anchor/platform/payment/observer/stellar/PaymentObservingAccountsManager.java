package org.stellar.anchor.platform.payment.observer.stellar;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.exception.ValueValidationException;
import org.stellar.anchor.platform.data.PaymentObservingAccount;
import org.stellar.anchor.util.Log;

public class PaymentObservingAccountsManager {
  Map<String, ObservingAccount> allAccounts;
  private final PaymentObservingAccountStore store;

  public PaymentObservingAccountsManager(PaymentObservingAccountStore store) {
    this.store = store;
    allAccounts = new ConcurrentHashMap<>();
  }

  @PostConstruct
  public void initialize() throws ValueValidationException {
    List<PaymentObservingAccount> accounts = store.list();
    for (PaymentObservingAccount account : accounts) {
      ObservingAccount oa =
          new ObservingAccount(account.getAccount(), account.getLastObserved(), true);
      upsert(oa);
    }

    // Start the eviction task
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(
        this::evictAndPersist, 60, getEvictPeriod().getSeconds(), TimeUnit.SECONDS);
  }

  public void evictAndPersist() {
    Log.info("Evicting old accounts...");
    this.evict(getEvictMaxAge());
    Log.info("Persisting accounts...");
    for (ObservingAccount account : this.getAccounts()) {
      store.upsert(account.account, account.lastObserved);
    }
  }

  /**
   * Adds an account to be observed. If the account is being observed, it will be updated.
   *
   * @param account The account being observed.
   * @param expiring true if the account can be expired. false, otherwise.
   */
  public void upsert(String account, Boolean expiring) throws ValueValidationException {
    if (account != null && expiring != null) {
      upsert(new ObservingAccount(account, Instant.now(), expiring));
    }
  }

  /**
   * Add an account to be observed. If the account is being observed, it will be updated.
   *
   * @param observingAccount The account being observed.
   */
  public void upsert(ObservingAccount observingAccount) throws ValueValidationException {
    if (observingAccount != null) {
      ObservingAccount existingAccount = allAccounts.get(observingAccount.account);
      if (existingAccount == null) {
        allAccounts.put(observingAccount.account, observingAccount);
        // update the database
        store.upsert(observingAccount.account, observingAccount.lastObserved);
      } else {
        if (!observingAccount.canExpire.equals(existingAccount.canExpire))
          throw new ValueValidationException(
              String.format(
                  "The expiring flag cannot be modified. Account=[%s]", observingAccount.account));
        existingAccount.account = observingAccount.account;
        existingAccount.lastObserved = observingAccount.lastObserved;
      }
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
   * @param maxIdleTime evict all accounts that are older than maxAge
   */
  public void evict(Duration maxIdleTime) {
    for (ObservingAccount acct : getAccounts()) {
      if (!acct.canExpire) continue;

      Duration idleTime = Duration.between(Instant.now(), acct.lastObserved).abs();
      if (idleTime.compareTo(maxIdleTime) > 0) {
        allAccounts.remove(acct.account);
        store.delete(acct.account);
      }
    }
  }

  @AllArgsConstructor
  public static class ObservingAccount {
    String account;
    Instant lastObserved;
    Boolean canExpire;
  }

  Duration getEvictPeriod() {
    return Duration.of(1, HOURS);
  }

  Duration getEvictMaxAge() {
    return Duration.of(1, DAYS);
  }
}
