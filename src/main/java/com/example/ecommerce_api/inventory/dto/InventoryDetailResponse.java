package com.example.ecommerce_api.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Detail view of one inventory record: current stock figures plus the most
 * recent stock movements, for the admin "inventory detail" page.
 */
@Data
@Builder
public class InventoryDetailResponse {

    private InventoryResponse inventory;

    private Integer totalStockIn;

    private Integer totalStockOut;

    private List<InventoryTransactionResponse> recentTransactions;
}
