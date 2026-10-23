package com.example.ecommerce_api.cart.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Builder
public class CartItemResponse {
    private Long id;

    private Long productId;
    private String productName;

    private Long productVariantId;
    private String sku;
    private String color;
    private String sizeName;
    private String imageUrl;
    /** false = product or option was archived; it must be removed before checkout. */
    private boolean available;

    private Integer quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;
}
