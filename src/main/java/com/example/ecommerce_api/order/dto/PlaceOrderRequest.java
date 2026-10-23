package com.example.ecommerce_api.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotNull
    private Long addressId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @Size(max = 50)
    private String couponCode;
}
