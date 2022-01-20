package org.stellar.anchor.paymentsservice;

@SuppressWarnings("unused")
public class Payment {
    String id;
    Network network;
    String sourceAccountId;
    String destinationAccountId;
    Balance balance;
    String status;
    String errorCode;
    String createdAt;
    String updatedAt;
}
