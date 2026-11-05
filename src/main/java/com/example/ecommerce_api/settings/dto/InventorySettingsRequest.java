package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventorySettingsRequest {

    /** Available stock at or below this counts as low stock; 0 = only out-of-stock alerts. */
    @NotNull(message = "Low-stock threshold is required")
    @Min(value = 0, message = "Low-stock threshold cannot be negative")
    @Max(value = 100000, message = "Low-stock threshold cannot be more than 100000")
    private Integer lowStockThreshold;
}
