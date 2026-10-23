package com.example.ecommerce_api.inventory.service;

import com.example.ecommerce_api.Enum.InventoryStatus;
import com.example.ecommerce_api.Enum.InventoryTransactionType;
import com.example.ecommerce_api.inventory.dto.InventoryStatsResponse;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.DuplicateResourceException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.dto.InventoryDetailResponse;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.dto.InventoryTransactionResponse;
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.inventory.entity.InventoryTransaction;
import com.example.ecommerce_api.inventory.mapper.InventoryMapper;
import com.example.ecommerce_api.inventory.repository.InventoryRepository;
import com.example.ecommerce_api.inventory.repository.InventoryTransactionRepository;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventory(Pageable pageable) {

        Page<InventoryResponse> page = inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toInventoryResponse);
        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> search(String search, InventoryStatus status, Pageable pageable) {

        String term = search == null ? "" : search.trim().toLowerCase();
        int low = InventoryMapper.LOW_STOCK_THRESHOLD;

        // Translate the status into a range of available quantity (same rules as InventoryMapper.toStatus)
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        if (status != null) {
            switch (status) {
                case OUT_OF_STOCK -> max = 0;
                case LOW_STOCK -> { min = 1; max = low; }
                case IN_STOCK -> min = low + 1;
            }
        }

        Page<InventoryResponse> page = inventoryRepository.search(term, min, max, pageable)
                .map(inventoryMapper::toInventoryResponse);
        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStatsResponse getStats() {

        int low = InventoryMapper.LOW_STOCK_THRESHOLD;

        return InventoryStatsResponse.builder()
                .totalVariants(inventoryRepository.count())
                .inStock(inventoryRepository.countByAvailableBetween(low + 1, Integer.MAX_VALUE))
                .lowStock(inventoryRepository.countByAvailableBetween(1, low))
                .outOfStock(inventoryRepository.countByAvailableBetween(Integer.MIN_VALUE, 0))
                .totalUnits(inventoryRepository.sumQuantity())
                .reservedUnits(inventoryRepository.sumReservedQuantity())
                .lowStockThreshold(low)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductVariantId(Long productVariantId) {
        return inventoryMapper.toInventoryResponse(findByVariant(productVariantId));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDetailResponse getDetail(Long productVariantId) {

        Inventory inventory = findByVariant(productVariantId);
        Long inventoryId = inventory.getId();

        List<InventoryTransaction> recent =
                inventoryTransactionRepository.findTop10ByInventory_IdOrderByCreatedAtDesc(inventoryId);

        return InventoryDetailResponse.builder()
                .inventory(inventoryMapper.toInventoryResponse(inventory))
                .totalStockIn(inventoryTransactionRepository.sumStockIn(inventoryId))
                .totalStockOut(inventoryTransactionRepository.sumStockOut(inventoryId))
                .recentTransactions(inventoryMapper.toTransactionResponse(recent))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStock(int threshold, Pageable pageable) {

        if (threshold < 0) {
            throw new BadRequestException("Threshold cannot be negative");
        }
        Page<InventoryResponse> page = inventoryRepository.findLowStock(threshold, pageable)
                .map(inventoryMapper::toInventoryResponse);
        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactions(Long productVariantId, Pageable pageable) {

        Inventory inventory = findByVariant(productVariantId);
        Page<InventoryTransactionResponse> page = inventoryTransactionRepository
                .findAllByInventory_Id(inventory.getId(), pageable)
                .map(inventoryMapper::toTransactionResponse);
        return new PageResponse<>(page);
    }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    @Override
    public InventoryResponse createForVariant(Long productVariantId, int initialQuantity) {

        ProductVariants variant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found with id: " + productVariantId));

        if (inventoryRepository.existsByProductVariant_Id(productVariantId)) {
            throw new DuplicateResourceException(
                    "Inventory already exists for product variant: " + productVariantId);
        }

        Inventory inventory = inventoryRepository.save(new Inventory(variant, initialQuantity));

        if (initialQuantity > 0) {
            logTransaction(inventory, InventoryTransactionType.PURCHASE, initialQuantity,
                    null, null, "Initial stock");
        }
        return inventoryMapper.toInventoryResponse(inventory);
    }

    @Override
    public InventoryResponse restock(Long productVariantId, int quantity, String note) {

        Inventory inventory = lockByVariant(productVariantId);
        inventory.increase(quantity);

        logTransaction(inventory, InventoryTransactionType.PURCHASE, quantity, null, null, note);
        return inventoryMapper.toInventoryResponse(inventoryRepository.saveAndFlush(inventory));
    }

    @Override
    public void reserve(Long productVariantId, int quantity) {
        // Reservation does not change on-hand stock, so no transaction is logged.
        lockByVariant(productVariantId).reserve(quantity);
    }

    @Override
    public void releaseReservation(Long productVariantId, int quantity) {
        lockByVariant(productVariantId).release(quantity);
    }

    @Override
    public void confirmSale(Long productVariantId, int quantity, String referenceType, Long referenceId) {

        Inventory inventory = lockByVariant(productVariantId);
        inventory.confirmSale(quantity);

        ProductVariants variant = inventory.getProductVariant();
        int sold = variant.getSoldQuantity() != null ? variant.getSoldQuantity() : 0;
        variant.setSoldQuantity(sold + quantity);

        logTransaction(inventory, InventoryTransactionType.SALE, -quantity, referenceType, referenceId, null);
    }

    @Override
    public InventoryResponse adjust(Long productVariantId, InventoryTransactionType type, int delta, String note) {

        validateAdjustment(type, delta);

        Inventory inventory = lockByVariant(productVariantId);
        if (delta > 0) {
            inventory.increase(delta);
        } else {
            inventory.decrease(-delta);
        }

        logTransaction(inventory, type, delta, null, null, note);
        return inventoryMapper.toInventoryResponse(inventoryRepository.saveAndFlush(inventory));
    }

    @Override
    public void receiveReturn(Long productVariantId, int quantity, boolean restock,
                              String referenceType, Long referenceId) {

        Inventory inventory = lockByVariant(productVariantId);

        ProductVariants variant = inventory.getProductVariant();
        int sold = variant.getSoldQuantity() != null ? variant.getSoldQuantity() : 0;
        variant.setSoldQuantity(Math.max(0, sold - quantity));

        if (restock) {
            inventory.increase(quantity);
            logTransaction(inventory, InventoryTransactionType.RETURN, quantity, referenceType, referenceId,
                    "Customer return");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validateAdjustment(InventoryTransactionType type, int delta) {
        if (delta == 0) {
            throw new BadRequestException("Quantity change cannot be 0");
        }
        switch (type) {
            case ADJUSTMENT -> { }
            case RETURN -> {
                if (delta < 0) throw new BadRequestException("RETURN must have a positive quantity change");
            }
            case DAMAGE, LOSS -> {
                if (delta > 0) throw new BadRequestException(type + " must have a negative quantity change");
            }
            default -> throw new BadRequestException(
                    type + " cannot be used for manual adjustment; use restock or order flow instead");
        }
    }

    private Inventory findByVariant(Long productVariantId) {
        return inventoryRepository.findByProductVariant_Id(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product variant: " + productVariantId));
    }

    /** Loads the inventory with a row lock so concurrent stock changes are serialized. */
    private Inventory lockByVariant(Long productVariantId) {
        return inventoryRepository.lockByProductVariant_Id(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product variant: " + productVariantId));
    }

    private void logTransaction(Inventory inventory, InventoryTransactionType type, int quantityChange,
                                String referenceType, Long referenceId, String note) {
        inventoryTransactionRepository.save(InventoryTransaction.builder()
                .inventory(inventory)
                .type(type)
                .quantityChange(quantityChange)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note)
                .build());
    }
}
