package org.stellar.anchor.paymentsservice;

import java.util.List;

@SuppressWarnings("unused")
public class PaymentHistory {
    Account account;
    String afterCursor;
    String beforeCursor;
    List<Payment> payments;
}
