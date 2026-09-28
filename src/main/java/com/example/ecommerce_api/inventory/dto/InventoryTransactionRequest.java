package com.example.ecommerce_api.inventory.dto;

import com.example.ecommerce_api.Enum.InventoryTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InventoryTransactionRequest {

    @NotNull(message = "Inventory ID is required")
    private Long inventoryId;

    @NotNull(message = "Transaction type is required")
    private InventoryTransactionType type;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 30, message = "Reference type must not exceed 30 characters")
    private String referenceType;

    private Long referenceId;

    @Size(max = 255, message = "Note must not exceed 255 characters")
    private String note;
}
