package com.example.ecommerce_api.returns.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** What the customer may still return from one order. */
@Getter
@Builder
public class ReturnEligibilityResponse {
    private String orderNumber;
    private boolean eligible;
    /** Why not, when eligible is false. */
    private String reason;
    private LocalDateTime returnableUntil;
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private Long orderItemId;
        private String productName;
        private String color;
        private String sizeName;
        private String imageUrl;
        private BigDecimal unitPrice;
        private Integer purchasedQuantity;
        private Integer returnableQuantity;
    }
}
