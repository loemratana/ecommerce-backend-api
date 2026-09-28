package com.example.ecommerce_api.product.controller;


import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.product.dto.request.ProductSizeRequest;
import com.example.ecommerce_api.product.dto.request.ProductSizeUpdateRequest;
import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;
import com.example.ecommerce_api.product.sevices.ProductSizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-sizes")
@RequiredArgsConstructor
public class ProductSizeController {

    private final ProductSizeService productSizeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductSizeResponse>> create(@Valid @RequestBody ProductSizeRequest request) {

        ProductSizeResponse response = productSizeService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product size created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody ProductSizeUpdateRequest request) {

        ProductSizeResponse response = productSizeService.update(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Product size updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {

        productSizeService.delete(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Product size deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> getById(@PathVariable Long id) {

        ProductSizeResponse response = productSizeService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSizeResponse>>> getAll() {

        List<ProductSizeResponse> response = productSizeService.getAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> activate(@PathVariable Long id) {

        ProductSizeResponse response = productSizeService.activate(id);

        return ResponseEntity.ok(ApiResponse.success(response, "Product size activated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductSizeResponse>> deactivate(@PathVariable Long id) {

        ProductSizeResponse response = productSizeService.deactivate(id);

        return ResponseEntity.ok(ApiResponse.success(response, "Product size deactivated successfully"));
    }
}
