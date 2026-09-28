package com.example.ecommerce_api.inventory.mapper;


import com.example.ecommerce_api.Enum.InventoryStatus;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import com.example.ecommerce_api.product.mapper.ProductVariantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryMapper {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductMapper productMapper;

    private final ProductVariantMapper productVariantMapper;

    public InventoryResponse toInventoryResponse(Inventory inventory) {

        if (inventory == null) {
            return null;
        }

        ProductVariants productVariant = inventory.getProductVariant();

        int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int reservedQuantity = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
        int availableQuantity = Math.max(quantity - reservedQuantity, 0);

        InventoryResponse.InventoryResponseBuilder builder = InventoryResponse.builder()
                .id(inventory.getId())
                .quantity(quantity)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(availableQuantity)
                .status(toStatus(availableQuantity))
                .updatedAt(inventory.getUpdatedAt());

        if (productVariant != null) {
            builder.product(productMapper.toProductSummary(productVariant.getProduct()))
                    .variant(productVariantMapper.toResponse(productVariant))
                    .price(productVariant.getPriceAfterDiscount() != null
                            ? productVariant.getPriceAfterDiscount()
                            : productVariant.getPrice())
                    .soldQuantity(productVariant.getSoldQuantity() != null ? productVariant.getSoldQuantity() : 0);
        }

        return builder.build();
    }

    public List<InventoryResponse> toInventoryResponse(List<Inventory> inventories) {
        return inventories.stream().map(this::toInventoryResponse).toList();
    }

    private InventoryStatus toStatus(int availableQuantity) {
        if (availableQuantity <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (availableQuantity <= LOW_STOCK_THRESHOLD) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.IN_STOCK;
    }
}
