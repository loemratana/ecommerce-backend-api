package com.example.ecommerce_api.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/** Body for restocking a variant; the variant id comes from the URL path. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;
}
