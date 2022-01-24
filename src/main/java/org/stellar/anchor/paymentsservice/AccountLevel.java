package org.stellar.anchor.paymentsservice;

import lombok.Getter;

@Getter
public enum AccountLevel {
    DISTRIBUTION("distribution", "the account owned by the admin"),
    DEFAULT("default", "a regular end-user account");

    private final String name;
    private final String description;

    AccountLevel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
