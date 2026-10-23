package com.example.ecommerce_api.inventory.controller;



import com.example.ecommerce_api.Enum.InventoryStatus;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.inventory.dto.InventoryStatsResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.inventory.service.InventoryService;
import com.example.ecommerce_api.inventory.dto.CreateInventoryRequest;
import com.example.ecommerce_api.inventory.dto.InventoryDetailResponse;
import com.example.ecommerce_api.inventory.dto.InventoryResponse;
import com.example.ecommerce_api.inventory.dto.InventoryTransactionResponse;
import com.example.ecommerce_api.inventory.dto.StockAdjustmentRequest;
import com.example.ecommerce_api.inventory.dto.StockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/inventory")
@Tag(name = "inventory")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Get all inventory records",
            description = "Paginated list sorted by last update. Optional search (product / SKU / color) and status filter")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getInventory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        return ResponseEntity.ok(ApiResponse.success(inventoryService.search(search, status, pageable)));
    }

    @Operation(summary = "Get inventory stats", description = "Variant counts per stock status and unit totals")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InventoryStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getStats()));
    }

    @Operation(summary = "Get low-stock inventory",
            description = "Variants whose available quantity (quantity - reserved) is at or below the threshold")
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "quantity"));

        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStock(threshold, pageable)));
    }

    @Operation(summary = "Get inventory by product variant id")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    @GetMapping("/{productVariantId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(@PathVariable Long productVariantId) {
        InventoryResponse response = inventoryService.getByProductVariantId(productVariantId);

        return ResponseEntity.ok().body(ApiResponse.success(response));
    }

    @Operation(summary = "Get inventory detail view",
            description = "Stock figures, total stock in/out and the 10 most recent stock movements")
    @GetMapping("/{productVariantId}/detail")
    public ResponseEntity<ApiResponse<InventoryDetailResponse>> getDetail(@PathVariable Long productVariantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getDetail(productVariantId)));
    }

    @Operation(summary = "Get stock movement history", description = "Paginated transactions, newest first")
    @GetMapping("/{productVariantId}/transactions")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> getTransactions(
            @PathVariable Long productVariantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(inventoryService.getTransactions(productVariantId, pageable)));
    }

    @Operation(summary = "Create inventory for a product variant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Inventory created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product variant not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Inventory already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> create(@Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse response = inventoryService.createForVariant(
                request.getProductVariantId(), request.getInitialQuantity());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Inventory created"));
    }

    @Operation(summary = "Restock a product variant", description = "Adds units to on-hand stock and logs a PURCHASE")
    @PostMapping("/{productVariantId}/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(
            @PathVariable Long productVariantId,
            @Valid @RequestBody StockRequest request) {

        InventoryResponse response = inventoryService.restock(
                productVariantId, request.getQuantity(), request.getNote());

        return ResponseEntity.ok(ApiResponse.success(response, "Stock added"));
    }

    @Operation(summary = "Adjust stock manually",
            description = "ADJUSTMENT (+/-), RETURN (+), DAMAGE (-), LOSS (-)")
    @PostMapping("/{productVariantId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjust(
            @PathVariable Long productVariantId,
            @Valid @RequestBody StockAdjustmentRequest request) {

        InventoryResponse response = inventoryService.adjust(
                productVariantId, request.getType(), request.getQuantityChange(), request.getNote());

        return ResponseEntity.ok(ApiResponse.success(response, "Stock adjusted"));
    }
}
