package com.example.ecommerce_api.brand.controller;


import com.example.ecommerce_api.brand.dto.request.BrandRequest;
import com.example.ecommerce_api.brand.dto.response.BrandResponse;
import com.example.ecommerce_api.brand.service.BrandService;
import com.example.ecommerce_api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {

        BrandResponse response = brandService.createBrand(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Brand created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(@PathVariable Long id,
                                                                    @Valid @RequestBody BrandRequest request) {

        BrandResponse response = brandService.updateBrand(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Brand updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {

        brandService.deleteBrand(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Brand deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {

        BrandResponse response = brandService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {

        List<BrandResponse> response = brandService.findAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
