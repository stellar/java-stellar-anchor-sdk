package org.stellar.anchor.paymentservice.circle.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.stellar.anchor.paymentservice.Payment;

@Getter
public enum CirclePaymentStatus {
    @SerializedName("pending")
    PENDING("pending"),

    @SerializedName("complete")
    COMPLETE("complete"),

    @SerializedName("failed")
    FAILED("failed");

    private final String name;

    CirclePaymentStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Payment.Status toPaymentStatus() {
        switch (this) {
            case PENDING:
                return Payment.Status.PENDING;
            case COMPLETE:
                return Payment.Status.SUCCESSFUL;
            case FAILED:
                return Payment.Status.FAILED;
            default:
                throw new RuntimeException("unsupported CirclePaymentStatus -> Payment.Status conversion");
        }
    }
}
