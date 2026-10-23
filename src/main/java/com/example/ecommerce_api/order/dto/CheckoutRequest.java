package com.example.ecommerce_api.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    @NotNull
    private Long addressId;

    @Size(max = 50)
    private String couponCode;
}
