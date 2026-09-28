package com.example.ecommerce_api.inventory;

import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.entity.Inventory;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryResponse getByProductVariantId(Long productVariantId);

    PageResponse<InventoryResponse> getInventory(
            Pageable pageable
    );
    InventoryResponse createForVariant(Long productVariantId, int initialQuantity);

    /** Adds to on-hand quantity (e.g. supplier delivery). Logs a PURCHASE transaction. */
    void restock(Long productVariantId, int quantity, String note);

    /** Moves units from available into reservedQuantity (e.g. added to an order pending payment). */
    void reserve(Long productVariantId, int quantity);

    /** Releases a previous reservation without selling the stock (e.g. cancelled order). */
    void releaseReservation(Long productVariantId, int quantity);

    /** Converts a reservation into an actual sale: decrements both quantity and reservedQuantity. */
    void confirmSale(Long productVariantId, int quantity, String referenceType, Long referenceId);

    /** Manual correction to on-hand quantity (positive or negative). Logs an ADJUSTMENT transaction. */
    void adjust(Long productVariantId, int delta, String note);
}
