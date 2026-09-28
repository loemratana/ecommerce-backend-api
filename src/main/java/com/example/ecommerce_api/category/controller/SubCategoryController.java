package com.example.ecommerce_api.category.controller;


import com.example.ecommerce_api.category.dto.request.SubCategoryRequest;
import com.example.ecommerce_api.category.dto.response.SubCategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryResponse;
import com.example.ecommerce_api.category.service.SubCategoryService;
import com.example.ecommerce_api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sub-categories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> createSubCategory(@Valid @RequestBody SubCategoryRequest request) {

        SubCategoryResponse response = subCategoryService.createSubCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Sub-category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> updateSubCategory(@PathVariable Long id,
                                                                                @Valid @RequestBody SubCategoryRequest request) {

        SubCategoryResponse response = subCategoryService.updateSubCategory(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Sub-category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSubCategory(@PathVariable Long id) {

        subCategoryService.deleteSubCategory(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Sub-category deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryResponse>> getSubCategoryById(@PathVariable Long id) {

        SubCategoryResponse response = subCategoryService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<SubCategoryDetailResponse>> getSubCategoryDetailById(@PathVariable Long id) {
        SubCategoryDetailResponse response = subCategoryService.getDetailSubCategory(id);
        return ResponseEntity.ok().body(ApiResponse.success(response));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubCategoryResponse>>> getAllSubCategories(
            @RequestParam(required = false) Long categoryId) {

        List<SubCategoryResponse> response = categoryId != null
                ? subCategoryService.findByCategoryId(categoryId)
                : subCategoryService.findAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
