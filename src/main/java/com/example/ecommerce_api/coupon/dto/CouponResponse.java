package com.example.ecommerce_api.coupon.dto;

import com.example.ecommerce_api.Enum.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponse {
    private Long id;

    private String code;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;

    private LocalDateTime expiryDate;

    private Boolean active;

    private Integer usageLimit;

    private Integer usedCount;

    private Integer remainingUses;

    /** Only set when validating/using a coupon against an order amount */
    private BigDecimal discountAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
