package com.example.ecommerce_api.Enum;

/** How the customer pays. Each value has one PaymentGateway strategy. */
public enum PaymentMethod {
    /** Stripe hosted Checkout; confirmed by asking Stripe when the customer returns. */
    STRIPE,
    /** Paid in cash to the courier; the payment is collected when the order is delivered. */
    CASH_ON_DELIVERY
}
