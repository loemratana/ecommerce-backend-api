package com.example.ecommerce_api.inventory.dto;

import com.example.ecommerce_api.Enum.InventoryTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionResponse {

    private Long id;

    private InventoryTransactionType type;

    /** Signed change to on-hand quantity: positive = stock in, negative = stock out. */
    private Integer quantityChange;

    private String referenceType;

    private Long referenceId;

    private String note;

    private LocalDateTime createdAt;
}
