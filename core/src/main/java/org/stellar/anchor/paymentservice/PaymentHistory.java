package org.stellar.anchor.paymentservice;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PaymentHistory {
  Account account;
  String cursor;
  List<Payment> payments = new ArrayList<>();

  public PaymentHistory(Account account) {
    this.account = account;
  }

  public PaymentHistory(Account account, String cursor, List<Payment> payments) {
    this.account = account;
    this.cursor = cursor;
    this.payments = payments;
  }
}
