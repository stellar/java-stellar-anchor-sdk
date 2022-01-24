package org.stellar.anchor.paymentsservice;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class Payment {
    String id;
    Account sourceAccount;
    Account destinationAccount;
    /**
     * The balance currency name contains the scheme of the destination network of the payment.
     */
    Balance balance;
    Status status;
    String errorCode;
    Date createdAt;
    Date updatedAt;
    Map<String, ?> originalResponse;

    public enum Status {
        PENDING("pending"),
        SUCCESSFUL("successful"),
        FAILED("failed");

        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
