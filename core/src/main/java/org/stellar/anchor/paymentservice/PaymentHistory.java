package org.stellar.anchor.paymentservice;

import java.util.List;

@SuppressWarnings("unused")
public class PaymentHistory {
  Account account;
  String afterCursor;
  String beforeCursor;
  List<Payment> payments;
}
