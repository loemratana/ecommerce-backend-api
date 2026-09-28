package com.example.ecommerce_api.inventory.dto;

import com.example.ecommerce_api.Enum.InventoryStatus;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import com.example.ecommerce_api.product.dto.response.ProductVariantResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class InventoryResponse {

    private Long id;

    private ProductSummaryResponse product;

    private ProductVariantResponse variant;

    private BigDecimal price;

    private Integer quantity;

    private Integer reservedQuantity;

    private Integer availableQuantity;

    private Integer soldQuantity;

    private InventoryStatus status;

    private LocalDateTime updatedAt;
}
