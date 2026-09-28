package com.example.ecommerce_api.product.controller;


import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductStatsResponse;
import com.example.ecommerce_api.product.sevices.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product", description = "Add a new product with its variants and images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU or slug")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request); // assumes create method exists
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get product by ID", description = "Returns a single product with all details (brand, subCategory, variants, images)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @Operation(summary = "Get product dashboard stats",
            description = "Returns total/active/low-stock/out-of-stock product counts for the admin dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats computed successfully",
                    content = @Content(schema = @Schema(implementation = ProductStatsResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<ProductStatsResponse> getStats() {
        return ResponseEntity.ok(productService.getStats());
    }

    @Operation(summary = "List products with filtering and pagination",
            description = "Retrieve a paginated list of products. Supports filtering by title, brand, subCategory, price range, and minimum rating.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Filter by product title (partial match, case-insensitive)")
            @RequestParam(required = false) String title,

            @Parameter(description = "Filter by brand ID")
            @RequestParam(required = false) Long brandId,

            @Parameter(description = "Filter by sub-category ID")
            @RequestParam(required = false) Long subCategoryId,

            @Parameter(description = "Minimum price")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Minimum average rating (0.0 – 5.0)")
            @RequestParam(required = false) BigDecimal minRating,

            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProductResponse> page = productService.getAll(
                title, brandId, subCategoryId, minPrice, maxPrice, minRating, pageable
        );
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Update an existing product", description = "Update product details, variants, and associations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., priceAfterDiscount > price)"),
            @ApiResponse(responseCode = "409", description = "Slug or SKU conflict")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(summary = "Delete a product", description = "Deletes a product and all its associated variants and images (cascade)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
