package com.example.ecommerce_api.payment.gateway;

/** providerReference: the gateway's id (e.g. Checkout Session id). redirectUrl: where the customer pays, or null. */
public record PaymentInitResult(String providerReference, String redirectUrl) {

    /** Nothing to do online, e.g. cash on delivery. */
    public static PaymentInitResult offline() {
        return new PaymentInitResult(null, null);
    }
}
