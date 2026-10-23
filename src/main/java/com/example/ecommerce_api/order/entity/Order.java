package com.example.ecommerce_api.order.entity;


import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.coupon.entity.Coupon;
import com.example.ecommerce_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Status only changes through the transition methods (markPaid, cancel, ...), and the
 * money fields are always recalculated from the items, coupons, tax and shipping:
 *
 *   totalOrderPrice = subtotal - discountAmount + taxPrice + shippingPrice
 *
 * Items, coupons and charges can only be changed while the order is PENDING.
 */
@Getter
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user_date", columnList = "user_id, order_date"),
                @Index(name = "idx_order_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_number", columnNames = "order_number")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier shown to customers and used in URLs instead of the sequential id. */
    @Column(name = "order_number", nullable = false, updatable = false, length = 32)
    private String orderNumber;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Embedded
    private ShippingAddress shippingAddress;

    /** Sum of item line totals, before coupons. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Total taken off by coupons, never more than the subtotal. */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxPrice = BigDecimal.ZERO;

    @Column(name = "shipping_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingPrice = BigDecimal.ZERO;

    @Column(name = "total_order_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOrderPrice = BigDecimal.ZERO;

    /** Money given back through returns; revenue counts total minus this. */
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /** Starts the return period. */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;


    // Batched so the admin order list can count items without one query per order
    @Getter(AccessLevel.NONE)
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "order", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<OrderCoupon> orderCoupons = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;


    @CreationTimestamp
    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    public Order(User user, ShippingAddress shippingAddress) {
        this.user = user;
        this.shippingAddress = shippingAddress;
        this.orderNumber = generateOrderNumber();
    }

    public void choosePaymentMethod(PaymentMethod method) {
        requirePending("choose a payment method");
        this.paymentMethod = method;
    }

    public  void startProcessing()
    {
        if (status == OrderStatus.PENDING && paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            status = OrderStatus.PROCESSING;
            return;
        }
        transitionTo(OrderStatus.PROCESSING);
    }

    /** e.g. ORD-20261001-3F9A1C7B; uniqueness is enforced by uk_order_number. */
    private static String generateOrderNumber() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + LocalDate.now().format(ORDER_NUMBER_DATE) + "-" + random;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    public List<OrderCoupon> getOrderCoupons() {
        return Collections.unmodifiableList(orderCoupons);
    }

    // ----- building the order (PENDING only) --------------------------------------------------

    public void addItem(OrderItem item) {
        requirePending("add items");
        if (!orderCoupons.isEmpty()) {
            // Coupon amounts are calculated from the subtotal when applied.
            throw new BusinessException("Add all items before applying coupons");
        }
        item.setOrder(this);
        orderItems.add(item);
        recalculateTotals();
    }

    /**
     * Records the coupon and the amount it takes off the current subtotal. Checking that the
     * coupon is active, unexpired and under its usage limit is up to the caller.
     */
    public void applyCoupon(Coupon coupon) {
        requirePending("apply coupons");
        if (orderItems.isEmpty()) {
            throw new BusinessException("Cannot apply a coupon to an empty order");
        }
        boolean alreadyApplied = orderCoupons.stream()
                .anyMatch(oc -> oc.getCoupon().getId().equals(coupon.getId()));
        if (alreadyApplied) {
            throw new BusinessException("Coupon " + coupon.getCode() + " is already applied to this order");
        }
        if (!coupon.meetsMinimum(subtotal)) {
            throw new BusinessException("Order subtotal is below the minimum for coupon " + coupon.getCode());
        }
        orderCoupons.add(new OrderCoupon(this, coupon, coupon.calculateDiscount(subtotal)));
        recalculateTotals();
    }

    public void setCharges(BigDecimal taxPrice, BigDecimal shippingPrice) {
        requirePending("change tax or shipping");
        this.taxPrice = nonNegative(taxPrice, "Tax");
        this.shippingPrice = nonNegative(shippingPrice, "Shipping price");
        recalculateTotals();
    }

    private void recalculateTotals() {
        subtotal = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        discountAmount = orderCoupons.stream()
                .map(OrderCoupon::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(subtotal);
        totalOrderPrice = subtotal.subtract(discountAmount).add(taxPrice).add(shippingPrice);
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessException(field + " must be zero or more");
        }
        return value;
    }

    private void requirePending(String action) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("Cannot " + action + " on an order that is " + status);
        }
    }

    // ----- status transitions -----------------------------------------------------------------

    public void markPaid() {
        transitionTo(OrderStatus.PAID);
    }

    public void markShipped() {
        transitionTo(OrderStatus.SHIPPED);
    }

    public void markDelivered() {
        transitionTo(OrderStatus.DELIVERED);
        deliveredAt = LocalDateTime.now();
    }

    // ----- refunds (returns) ------------------------------------------------------------------

    /** What can still be refunded: everything paid except shipping, minus earlier refunds. */
    public BigDecimal getRefundableAmount() {
        return totalOrderPrice.subtract(shippingPrice).subtract(refundedAmount).max(BigDecimal.ZERO);
    }

    /**
     * Refund for returned goods worth {@code goodsValue} at line prices: coupons and tax are
     * shared out in proportion, shipping is not refunded, never more than is still refundable.
     */
    public BigDecimal refundFor(BigDecimal goodsValue) {
        if (subtotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal paidForGoods = subtotal.subtract(discountAmount).add(taxPrice);
        return goodsValue.multiply(paidForGoods)
                .divide(subtotal, 2, java.math.RoundingMode.HALF_UP)
                .min(getRefundableAmount());
    }

    public void recordRefund(BigDecimal amount) {
        if (status != OrderStatus.DELIVERED) {
            throw new BusinessException("Only delivered orders can be refunded");
        }
        if (amount.signum() < 0 || amount.compareTo(getRefundableAmount()) > 0) {
            throw new BusinessException("Refund of " + amount + " exceeds what is left to refund on order " + orderNumber);
        }
        refundedAmount = refundedAmount.add(amount);
    }

    /** Releasing reserved stock and returning coupon usage is up to the caller. */
    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    private void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessException("Cannot change order " + orderNumber + " from " + status + " to " + next);
        }
        status = next;
    }
}
