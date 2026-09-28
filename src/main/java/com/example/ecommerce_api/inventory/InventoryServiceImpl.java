package com.example.ecommerce_api.inventory;

import com.example.ecommerce_api.Enum.InventoryTransactionType;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.inventory.entity.InventoryTransaction;
import com.example.ecommerce_api.inventory.mapper.InventoryMapper;
import com.example.ecommerce_api.inventory.repository.InventoryRepository;
import com.example.ecommerce_api.inventory.repository.InventoryTransactionRepository;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;



    @Override
    public PageResponse<InventoryResponse> getInventory(Pageable pageable) {

        Page<InventoryResponse> page = inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toInventoryResponse);
        return new PageResponse<>(page);
    }

    @Override
    public InventoryResponse getByProductVariantId(Long productVariantId) {

        Inventory inventory = inventoryRepository.findByProductVariant_Id(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant not found"));
        return inventoryMapper.toInventoryResponse(inventory);
    }

    @Override
    public InventoryResponse createForVariant(Long productVariantId, int initialQuantity) {
        return null;
    }

    @Override
    @Transactional
    public void restock(Long productVariantId, int quantity, String note) {

        if (quantity <=0)
        {
            throw  new BadRequestException("Quantity must be greater than 0");
        }

        Inventory inventory = inventoryRepository.findByProductVariant_Id(productVariantId).
                orElseThrow(() -> new ResourceNotFoundException("Product Variant not found"));

        inventory.setQuantity(inventory.getQuantity() + quantity);

        InventoryTransaction inventoryTransaction = new InventoryTransaction();

        inventoryTransaction.setInventory(inventory);
        inventoryTransaction.setType(InventoryTransactionType.PURCHASE);
        inventoryTransaction.setNote(note);

        inventoryTransactionRepository.save(inventoryTransaction);

    }

    @Override
    public void reserve(Long productVariantId, int quantity) {

    }

    @Override
    public void releaseReservation(Long productVariantId, int quantity) {

    }

    @Override
    public void confirmSale(Long productVariantId, int quantity, String referenceType, Long referenceId) {

    }

    @Override
    public void adjust(Long productVariantId, int delta, String note) {

    }

}
