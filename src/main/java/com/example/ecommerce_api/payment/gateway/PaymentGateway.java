package com.example.ecommerce_api.payment.gateway;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.entity.Payment;

import java.math.BigDecimal;

/**
 * Strategy for one way of paying. Implementations only talk to their provider and translate its
 * data into ours; the business rules (amounts, order status, locking, idempotency, refund limits)
 * live in PaymentService so every gateway behaves the same.
 *
 * Add a gateway by writing a @Component that implements this; PaymentGatewayResolver picks it up.
 */
public interface PaymentGateway {

    PaymentMethod method();

    /** Starts the payment on the provider's side (e.g. creates a Stripe Checkout Session). */
    PaymentInitResult initiate(Order order, Payment payment);

    /** Stops a still-pending payment on the provider's side, e.g. expires a Checkout Session. */
    default void cancel(Payment payment) {
    }

    /** Asks the provider how a pending payment stands; null while it is still open or can't be checked. */
    default GatewayEvent checkStatus(Payment payment) {
        return null;
    }

    /** Gives back {@code amount} of a captured payment. */
    RefundResult refund(Payment payment, BigDecimal amount);
}
