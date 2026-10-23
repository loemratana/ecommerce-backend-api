package com.example.ecommerce_api.order.entity;


import com.example.ecommerce_api.coupon.entity.Coupon;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_coupons",
        indexes = {
                @Index(name = "idx_order_coupon_order", columnList = "order_id"),
                @Index(name = "idx_order_coupon_coupon", columnList = "coupon_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_coupon_order_coupon", columnNames = {"order_id", "coupon_id"})
        }
)
public class OrderCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /** Code at the time of use, so the order still shows it if the coupon is renamed. */
    @Column(name = "coupon_code", nullable = false, length = 50)
    private String couponCode;

    /** Money taken off the order by this coupon (works for both PERCENTAGE and FIXED coupons). */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    OrderCoupon(Order order, Coupon coupon, BigDecimal discountAmount) {
        this.order = order;
        this.coupon = coupon;
        this.couponCode = coupon.getCode();
        this.discountAmount = discountAmount;
    }
}
