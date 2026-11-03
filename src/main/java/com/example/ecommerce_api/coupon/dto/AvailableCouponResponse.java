package com.example.ecommerce_api.coupon.dto;

import com.example.ecommerce_api.Enum.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What shoppers see about a coupon they can use right now; no usage counts or admin fields. */
public record AvailableCouponResponse(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        LocalDateTime expiryDate) {
}
