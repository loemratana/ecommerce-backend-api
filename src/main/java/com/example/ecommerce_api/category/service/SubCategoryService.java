package com.example.ecommerce_api.category.service;

import com.example.ecommerce_api.category.dto.request.SubCategoryRequest;
import com.example.ecommerce_api.category.dto.response.SubCategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryResponse;

import java.util.List;

public interface SubCategoryService {

    SubCategoryResponse createSubCategory(SubCategoryRequest subCategoryRequest);

    SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest subCategoryRequest);

    void deleteSubCategory(Long id);

    SubCategoryDetailResponse getDetailSubCategory(Long id);
    List<SubCategoryResponse> getSubCategories();

    SubCategoryResponse findById(Long id);

    List<SubCategoryResponse> findAll();

    List<SubCategoryResponse> findByCategoryId(Long categoryId);
}
