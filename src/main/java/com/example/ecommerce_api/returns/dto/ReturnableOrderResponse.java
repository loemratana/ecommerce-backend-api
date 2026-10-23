package com.example.ecommerce_api.returns.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One of my orders that a return can still be requested for. */
@Getter
@Builder
public class ReturnableOrderResponse {
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime deliveredAt;
    /** null only for old orders delivered before the delivery date was recorded. */
    private LocalDateTime returnableUntil;
    private BigDecimal totalOrderPrice;
    /** Units purchased across all lines. */
    private int itemCount;
    /** Units not yet in an open or completed return. */
    private int returnableItemCount;
}
