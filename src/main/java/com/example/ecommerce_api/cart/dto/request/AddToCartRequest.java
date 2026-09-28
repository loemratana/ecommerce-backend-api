package com.example.ecommerce_api.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddToCartRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private Integer quantity;

}
