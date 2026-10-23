package com.example.ecommerce_api.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequest {

    @NotNull
    private Long productVariantId;

    @NotNull
    @Positive
    private Integer quantity;
}
