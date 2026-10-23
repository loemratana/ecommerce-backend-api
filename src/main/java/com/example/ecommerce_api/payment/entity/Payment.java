package com.example.ecommerce_api.payment.entity;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One attempt to pay for an order. The amount is copied from the order when the attempt starts,
 * so it is what the gateway was asked to charge. Status only changes through the methods below.
 */
@Getter
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order", columnList = "order_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /** ISO 4217, e.g. USD. */
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    /** The gateway's id for this payment, e.g. a Stripe Checkout Session id. */
    @Column(name = "provider_reference")
    private String providerReference;

    /** Where the customer goes to pay; null when there is nothing to do online (cash on delivery). */
    @Column(name = "redirect_url", length = 2048)
    private String redirectUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Payment(Order order, PaymentMethod method, BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.currency = currency;
    }

    /** Set once the gateway has created its side of the payment. */
    public void attachProvider(String providerReference, String redirectUrl) {
        requireStatus(PaymentStatus.PENDING, "attach a gateway reference");
        this.providerReference = providerReference;
        this.redirectUrl = redirectUrl;
    }

    public void markSucceeded() {
        transitionTo(PaymentStatus.SUCCEEDED);
        paidAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        transitionTo(PaymentStatus.FAILED);
        failureReason = truncate(reason);
    }

    public void markExpired() {
        transitionTo(PaymentStatus.EXPIRED);
    }

    public void markCancelled() {
        transitionTo(PaymentStatus.CANCELLED);
    }

    public BigDecimal getRefundableAmount() {
        return status.isCaptured() ? amount.subtract(refundedAmount) : BigDecimal.ZERO;
    }

    public void recordRefund(BigDecimal refund) {
        if (refund == null || refund.signum() <= 0 || refund.compareTo(getRefundableAmount()) > 0) {
            throw new BusinessException("Refund of " + refund + " exceeds what is left to refund on payment " + id);
        }
        refundedAmount = refundedAmount.add(refund);
        transitionTo(refundedAmount.compareTo(amount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
    }

    private void requireStatus(PaymentStatus expected, String action) {
        if (status != expected) {
            throw new BusinessException("Cannot " + action + " on a payment that is " + status);
        }
    }

    private void transitionTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessException("Cannot change payment " + id + " from " + status + " to " + next);
        }
        status = next;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= 500 ? value : value.substring(0, 500);
    }
}
