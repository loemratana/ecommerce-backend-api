package com.example.ecommerce_api.product.dto.request;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantsRequest {

    private Long id;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    private String color;

    private Long sizeId;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount price cannot be negative")
    private BigDecimal priceAfterDiscount;

    /**
     * Opening stock for a NEW variant (ignored for existing ones; change their stock from the
     * inventory page so every change is logged).
     */
    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock;
}
