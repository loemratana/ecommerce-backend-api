package com.example.ecommerce_api.product.dto.response;

import com.example.ecommerce_api.Enum.CatalogStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Builder
public class ProductVariantResponse {

    private Long id;
    private CatalogStatus status;

    private String color;

    private ProductSizeResponse size;

    private String sku;

    private BigDecimal price;

    private BigDecimal priceAfterDiscount;

    private Integer soldQuantity;
    /** Stock the customer can still order (quantity - reserved). */
    private Integer availableQuantity;
}
