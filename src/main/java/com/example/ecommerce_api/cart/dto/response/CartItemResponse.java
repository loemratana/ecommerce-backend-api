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

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal lineTotal;
}
