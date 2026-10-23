package com.example.ecommerce_api.coupon.entity;


import com.example.ecommerce_api.Enum.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "coupons",
        indexes = {
                @Index(name = "idx_coupon_code", columnList = "code")
        }
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    // Percent (0.01-100) for PERCENTAGE, money amount for FIXED
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    // Order subtotal required to use this coupon; null = no minimum
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;


    // Maximum number of times this coupon can be used
    @Column(name = "usage_limit")
    private Integer usageLimit;

    // Number of times this coupon has already been used
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean meetsMinimum(BigDecimal orderAmount) {
        return minOrderAmount == null || orderAmount.compareTo(minOrderAmount) >= 0;
    }

    /** Discount for the given order subtotal, never more than the subtotal itself. */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        BigDecimal discount = discountType == DiscountType.FIXED
                ? discountValue
                : orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return discount.min(orderAmount);
    }
}
