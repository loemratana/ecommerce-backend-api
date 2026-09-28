package com.example.ecommerce_api.inventory.controller;



import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.InventoryService;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/inventory")
@Tag(name = "inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(description = "Get all inventory records")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(pageable)));
    }


    @Operation(description = "Get Product Inventory by Id ")
    @GetMapping("/{productVariantId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productVariantId) {
        InventoryResponse response = inventoryService.getByProductVariantId(productVariantId);

        return ResponseEntity.ok().body(ApiResponse.success(response));
    }
}
