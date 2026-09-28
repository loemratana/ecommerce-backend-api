package com.example.ecommerce_api.category.controller;


import com.example.ecommerce_api.category.dto.request.CategoryRequest;
import com.example.ecommerce_api.category.dto.response.CategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.CategoryResponse;
import com.example.ecommerce_api.category.service.CategoryService;
import com.example.ecommerce_api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable Long id,
                                                                          @Valid @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {

        CategoryResponse response = categoryService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> getCategoryDetailById(@PathVariable Long id) {

        CategoryDetailResponse response = categoryService.findDetailById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {

        List<CategoryResponse> response = categoryService.findAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
