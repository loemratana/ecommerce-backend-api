package com.example.ecommerce_api.payment.gateway;

import java.math.BigDecimal;

/**
 * A provider status change translated into our terms; amount is in major units (e.g. 12.50), or null if unknown.
 * For PAYMENT_SUCCEEDED it is the amount paid; for REFUNDED it is the total refunded so far (not just
 * this refund), so a repeated or out-of-order event can't count a refund twice.
 */
public record GatewayEvent(String eventId, Type type, String providerReference, BigDecimal amount, String failureReason) {

    public enum Type { PAYMENT_SUCCEEDED, PAYMENT_FAILED, PAYMENT_EXPIRED, REFUNDED, IGNORED }

    /** An event type we don't act on. */
    public static GatewayEvent ignored(String eventId) {
        return new GatewayEvent(eventId, Type.IGNORED, null, null, null);
    }
}
