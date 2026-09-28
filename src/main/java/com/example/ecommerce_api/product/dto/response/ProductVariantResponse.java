package com.example.ecommerce_api.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Builder
public class ProductVariantResponse {

    private Long id;

    private String color;

    private ProductSizeResponse size;

    private String sku;

    private BigDecimal price;

    private BigDecimal priceAfterDiscount;

    private Integer soldQuantity;
}
