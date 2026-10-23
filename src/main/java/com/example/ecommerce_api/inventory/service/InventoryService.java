package com.example.ecommerce_api.inventory.service;

import com.example.ecommerce_api.Enum.InventoryStatus;
import com.example.ecommerce_api.Enum.InventoryTransactionType;
import com.example.ecommerce_api.inventory.dto.InventoryStatsResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.dto.InventoryDetailResponse;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.dto.InventoryTransactionResponse;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryResponse getByProductVariantId(Long productVariantId);

    /** Stock figures + totals + the 10 most recent movements for one variant. */
    InventoryDetailResponse getDetail(Long productVariantId);

    PageResponse<InventoryResponse> getInventory(
            Pageable pageable
    );

    /** Admin table: optional text search (product title / SKU / color) and stock status filter. */
    PageResponse<InventoryResponse> search(String search, InventoryStatus status, Pageable pageable);

    InventoryStatsResponse getStats();

    /** Variants whose available stock is at or below the threshold. */
    PageResponse<InventoryResponse> getLowStock(int threshold, Pageable pageable);

    /** Full, paginated stock movement history of one variant. */
    PageResponse<InventoryTransactionResponse> getTransactions(Long productVariantId, Pageable pageable);

    InventoryResponse createForVariant(Long productVariantId, int initialQuantity);

    /** Adds to on-hand quantity (e.g. supplier delivery). Logs a PURCHASE transaction. */
    InventoryResponse restock(Long productVariantId, int quantity, String note);

    /** Moves units from available into reservedQuantity (e.g. added to an order pending payment). */
    void reserve(Long productVariantId, int quantity);

    /** Releases a previous reservation without selling the stock (e.g. cancelled order). */
    void releaseReservation(Long productVariantId, int quantity);

    /** Converts a reservation into an actual sale: decrements both quantity and reservedQuantity. */
    void confirmSale(Long productVariantId, int quantity, String referenceType, Long referenceId);

    /**
     * Manual correction to on-hand quantity. The type decides which sign is allowed:
     * ADJUSTMENT (+/-), RETURN (+), DAMAGE (-), LOSS (-).
     */
    InventoryResponse adjust(Long productVariantId, InventoryTransactionType type, int delta, String note);

    /**
     * A returned item arrived: it comes off the variant's sold count, and if it is sellable
     * ({@code restock}) it goes back into on-hand stock with a RETURN transaction.
     */
    void receiveReturn(Long productVariantId, int quantity, boolean restock, String referenceType, Long referenceId);
}
