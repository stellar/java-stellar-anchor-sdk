package org.stellar.anchor.platform.payment.observer.stellar;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.exception.AlreadyExistsException;

public class PaymentObservingAccountsManager {
  PriorityQueue<ObservingAccount> expiringAccounts;
  HashMap<String, ObservingAccount> allAccounts;
  private PaymentObservingAccountStore store;

  public PaymentObservingAccountsManager(PaymentObservingAccountStore store) {
    this.store = store;
    allAccounts = new HashMap<>();
    expiringAccounts = new PriorityQueue<>(Comparator.comparing(account -> account.startAt));
  }

  @PostConstruct
  void initiialize() {
    System.out.println("post construct");
  }

  /**
   * Adds an account to be observed.
   *
   * @param account The account being observed.
   * @param expiring true if the account can be expired. false, otherwise.
   */
  public void add(String account, Boolean expiring) throws AlreadyExistsException {
    add(new ObservingAccount(account, Instant.now(), expiring));
  }

  /**
   * Addes an account to be observed.
   *
   * @param observingAccount The account being observed.
   */
  public void add(ObservingAccount observingAccount) throws AlreadyExistsException {
    synchronized (this) {
      if (allAccounts.get(observingAccount.account) != null)
        throw new AlreadyExistsException(
            String.format("The account %s is already being observed", observingAccount.account));

      if (observingAccount.expiring) {
        expiringAccounts.add(observingAccount);
      }
      allAccounts.put(observingAccount.account, observingAccount);

      // we only manage expiring accounts.
      if (observingAccount.expiring) {
        store.add(observingAccount.account, observingAccount.startAt);
      }
    }
  }

  /**
   * Remove the account from the observed account list.
   *
   * @param account The account being removed.
   */
  public void remove(String account) {
    synchronized (this) {
      ObservingAccount targetAccount = allAccounts.get(account);
      if (targetAccount != null) {
        allAccounts.remove(account);
        expiringAccounts.remove(targetAccount);
        store.remove(account);
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
   * Check if the account is being observed.
   *
   * @param account The account to be checked.
   * @return true if the account is being observed. false, otherwise.
   */
  public boolean isObserving(String account) {
    return allAccounts.get(account) != null;
  }

  /**
   * Purge expired accounts
   *
   * @param maxAge purge all accounts that are older than maxAge
   */
  public void purge(Duration maxAge) {
    synchronized (this) {
      do {
        ObservingAccount oldest = expiringAccounts.peek();
        if (oldest == null) break; // the list is empty

        Duration age = Duration.between(Instant.now(), oldest.startAt).abs();
        if (age.compareTo(maxAge) < 0) break; // nothing older

        // remove
        expiringAccounts.poll();
        allAccounts.remove(oldest.account);
      } while (true);
    }
  }

  @AllArgsConstructor
  public static class ObservingAccount {
    String account;
    Instant startAt;

    Boolean expiring;
  }
}
