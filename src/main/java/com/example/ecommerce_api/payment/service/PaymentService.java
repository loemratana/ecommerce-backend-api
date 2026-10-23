package com.example.ecommerce_api.payment.service;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.dto.InitiatePaymentRequest;
import com.example.ecommerce_api.payment.dto.PaymentResponse;
import com.example.ecommerce_api.payment.gateway.GatewayEvent;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface PaymentService {

    Set<PaymentMethod> availableMethods();

    // customer
    /** Starts paying a PENDING order; reuses an open attempt with the same method. */
    PaymentResponse initiate(InitiatePaymentRequest request);

    List<PaymentResponse> getMyOrderPayments(String orderNumber);

    /**
     * Asks the gateway about the order's pending online payments and applies what it reports
     * (e.g. marks the order paid). Called when the customer returns from Stripe; safe to repeat.
     */
    List<PaymentResponse> confirmMyOrderPayments(String orderNumber);

    // admin
    /** Null status or method means any. */
    PageResponse<PaymentResponse> getAll(PaymentStatus status, PaymentMethod method, Pageable pageable);

    List<PaymentResponse> getOrderPayments(String orderNumber);

    // gateways
    /** Applies a status change reported by a gateway; repeats of an already-processed event are ignored. */
    void handleGatewayEvent(PaymentMethod method, GatewayEvent event);

    // order lifecycle, called by OrderServiceImpl / ReturnServiceImpl inside their transaction
    /** Cancels open payment attempts (e.g. expires the Stripe session) and refunds anything already paid. */
    void onOrderCancelled(Order order);

    /**
     * Gives {@code amount} back through the order's captured payments (newest first), each via its own
     * gateway. Returns what was refunded through payments; less than asked when the order has no
     * captured payment, e.g. it was marked paid by an admin. Throws if a gateway refuses, so the
     * caller's transaction rolls back. The caller must hold the order lock.
     */
    BigDecimal refund(Order order, BigDecimal amount, String reason);

    /** Cash on delivery: the courier has collected the money. */
    void onOrderDelivered(Order order);
}
