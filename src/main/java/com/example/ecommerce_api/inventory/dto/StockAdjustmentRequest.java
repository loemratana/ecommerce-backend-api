package com.example.ecommerce_api.inventory.dto;

import com.example.ecommerce_api.Enum.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual stock correction. The allowed types and the sign of quantityChange are:
 * ADJUSTMENT -> positive or negative (stock count correction)
 * RETURN     -> positive (customer returned items to stock)
 * DAMAGE     -> negative (broken items written off)
 * LOSS       -> negative (missing / stolen items)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Adjustment type is required")
    private InventoryTransactionType type;

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;
}
