package org.stellar.anchor.paymentservice;

import java.util.List;

public class PaymentHistory {
  Account account;
  String cursor;
  List<Payment> payments;

  public PaymentHistory() {}

  public PaymentHistory(Account account, String cursor, List<Payment> payments) {
    this.account = account;
    this.cursor = cursor;
    this.payments = payments;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public List<Payment> getPayments() {
    return payments;
  }

  public void setPayments(List<Payment> payments) {
    this.payments = payments;
  }
}
