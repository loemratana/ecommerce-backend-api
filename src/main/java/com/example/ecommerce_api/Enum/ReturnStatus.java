package com.example.ecommerce_api.Enum;

/**
 * REQUESTED -> APPROVED -> RECEIVED (stock + refund handled).
 * The customer can cancel while REQUESTED; the admin can reject while REQUESTED or APPROVED.
 */
public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    RECEIVED,
    CANCELLED;

    public boolean canTransitionTo(ReturnStatus next) {
        return switch (this) {
            case REQUESTED -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == RECEIVED || next == REJECTED;
            case REJECTED, RECEIVED, CANCELLED -> false;
        };
    }

    /** Statuses whose items still count against what can be returned from an order line. */
    public static java.util.Set<ReturnStatus> open() {
        return java.util.EnumSet.of(REQUESTED, APPROVED, RECEIVED);
    }
}
