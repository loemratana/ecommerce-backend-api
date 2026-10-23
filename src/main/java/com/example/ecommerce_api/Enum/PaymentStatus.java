package com.example.ecommerce_api.Enum;

/**
 * PENDING -> SUCCEEDED -> PARTIALLY_REFUNDED -> REFUNDED.
 * A PENDING payment can instead end as FAILED, EXPIRED (the gateway's time limit ran out) or
 * CANCELLED (the order was cancelled or the customer switched to another method).
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case PENDING -> next == SUCCEEDED || next == FAILED || next == EXPIRED || next == CANCELLED;
            case SUCCEEDED, PARTIALLY_REFUNDED -> next == PARTIALLY_REFUNDED || next == REFUNDED;
            case FAILED, EXPIRED, CANCELLED, REFUNDED -> false;
        };
    }

    /** Money was received (and possibly partly given back). */
    public boolean isCaptured() {
        return this == SUCCEEDED || this == PARTIALLY_REFUNDED || this == REFUNDED;
    }
}
