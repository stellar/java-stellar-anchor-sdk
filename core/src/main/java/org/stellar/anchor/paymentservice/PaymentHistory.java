package org.stellar.anchor.paymentservice;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PaymentHistory {
  Account account;
  String afterCursor;
  String beforeCursor;
  List<Payment> payments = new ArrayList<>();

  public PaymentHistory(Account account) {
    this.account = account;
  }
}
