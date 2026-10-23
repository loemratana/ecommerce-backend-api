package com.example.ecommerce_api.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class OrderCouponResponse {
    private Long id;
    private String couponCode;
    private BigDecimal discountAmount;
}
