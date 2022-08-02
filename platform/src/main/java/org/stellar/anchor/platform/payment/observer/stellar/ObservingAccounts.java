package org.stellar.anchor.platform.payment.observer.stellar;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import lombok.AllArgsConstructor;
import org.stellar.anchor.api.exception.AlreadyExistsException;

public class ObservingAccounts {
  PriorityQueue<ObservedAccount> expiringAccounts;
  HashMap<String, ObservedAccount> allAccounts;

  public ObservingAccounts() {
    allAccounts = new HashMap<>();
    expiringAccounts = new PriorityQueue<>(Comparator.comparing(account -> account.startAt));
  }

  /**
   * Adds an account to be observed.
   *
   * @param account The account being observed.
   * @param expiring true if the account can be expired. false, otherwise.
   */
  public void add(String account, Boolean expiring) throws AlreadyExistsException {
    if (allAccounts.get(account) != null)
      throw new AlreadyExistsException(
          String.format("The account %s is already being observed", account));
    add(new ObservedAccount(account, Instant.now(), expiring));
  }

  /**
   * Addes an account to be observed.
   *
   * @param observedAccount The account being observed.
   */
  public void add(ObservedAccount observedAccount) {
    synchronized (this) {
      if (observedAccount.expiring) {
        expiringAccounts.add(observedAccount);
      }
      allAccounts.put(observedAccount.account, observedAccount);
    }
  }

  /**
   * Remove the account from the observed account list.
   *
   * @param account The account being removed.
   */
  public void remove(String account) {
    synchronized (this) {
      ObservedAccount targetAccount = allAccounts.get(account);
      if (targetAccount != null) {
        allAccounts.remove(account);
        expiringAccounts.remove(targetAccount);
      }
    }
  }

  /**
   * Gets the list of observed accounts.
   *
   * @return The list of observed accounts.
   */
  public List<ObservedAccount> getAccounts() {
    return new ArrayList<>(allAccounts.values());
  }

  /**
   * Check if the account is being observed.
   *
   * @param account The account to be checked.
   * @return true if the account is being observed. false, otherwise.
   */
  public boolean isObserved(String account) {
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
        ObservedAccount oldest = expiringAccounts.peek();
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
  public static class ObservedAccount {
    String account;
    Instant startAt;

    Boolean expiring;
  }
}
