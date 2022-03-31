package org.stellar.anchor.model;

public enum TransactionStatus {
    PENDING_ANCHOR("pending_anchor", "processing"),
    PENDING_TRUST("pending_trust", "waiting for a trustline to be established"),
    PENDING_USER("pending_user", "waiting on user action"),
    PENDING_USR_TRANSFER_START(
            "pending_user_transfer_start", "waiting on the user to transfer funds"),
    PENDING_USR_TRANSFER_COMPLETE(
            "pending_user_transfer_complete", "the user has transferred the funds"),
    INCOMPLETE("incomplete", "incomplete"),
    NO_MARKET("no_market", "no market for the asset"),
    TOO_SMALL("too_small", "the transaction amount is too small"),
    TOO_LARGE("too_large", "the transaction amount is too big"),
    PENDING_SENDER("pending_sender", null),
    PENDING_RECEIVER("pending_receiver", null),
    PENDING_TRANSACTION_INFO_UPDATE("pending_transaction_info_update", null),
    PENDING_CUSTOMER_INFO_UPDATE("pending_customer_info_update", "waiting for more transaction information"),
    COMPLETED("completed", "complete"),
    ERROR("error", "error"),
    PENDING_EXTERNAL("pending_external", "waiting on an external entity"),
    PENDING_STELLAR("pending_stellar", "stellar is executing the transaction");

    private final String name;
    private final String description;

    TransactionStatus(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String toString() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
