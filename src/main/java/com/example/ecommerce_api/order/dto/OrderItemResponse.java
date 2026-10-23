package com.example.ecommerce_api.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Builder
public class OrderItemResponse {

    private Long id;

    private Long productVariantId;

    private String productName;
    private String sku;
    private String color;
    private String sizeName;
    private String imageUrl;

    private Integer quantity;

    private BigDecimal price;
    private BigDecimal priceAfterDiscount;
    private BigDecimal lineTotal;
}
