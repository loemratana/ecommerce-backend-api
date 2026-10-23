package com.example.ecommerce_api.inventory.dto;

import lombok.Builder;
import lombok.Data;

/** Counts for the admin inventory cards and dashboard widget. */
@Data
@Builder
public class InventoryStatsResponse {

    private long totalVariants;

    private long inStock;

    private long lowStock;

    private long outOfStock;

    /** Sum of on-hand units across all variants. */
    private long totalUnits;

    /** Sum of units held for unpaid orders. */
    private long reservedUnits;

    private int lowStockThreshold;
}
