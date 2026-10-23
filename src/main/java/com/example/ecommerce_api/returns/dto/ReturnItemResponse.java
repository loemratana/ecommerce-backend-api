package com.example.ecommerce_api.returns.dto;

import com.example.ecommerce_api.Enum.ReturnItemCondition;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReturnItemResponse {
    private Long id;
    private Long orderItemId;
    private Long productVariantId;
    private String productName;
    private String sku;
    private String color;
    private String sizeName;
    private String imageUrl;
    private Integer quantity;
    /** Unit price the customer paid for this line (before order-level coupons). */
    private BigDecimal unitPrice;
    /** Null until the items are received. */
    private ReturnItemCondition condition;
}
