package com.example.ecommerce_api.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStatsResponse {

    private long totalProducts;
    private long newThisMonth;

    private long activeProducts;
    private double activeProductsPercent;

    private long lowStock;
    private long outOfStock;
}
