package com.example.ecommerce_api.category.service;

import com.example.ecommerce_api.category.dto.request.CategoryRequest;
import com.example.ecommerce_api.category.dto.response.CategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest categoryRequest);

    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);

    void deleteCategory(Long id);

    CategoryResponse findById(Long id);

    CategoryDetailResponse findDetailById(Long id);

    List<CategoryResponse> findAll();
}
