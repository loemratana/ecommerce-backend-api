package com.example.ecommerce_api.category.mapper;


import com.example.ecommerce_api.category.dto.request.CategoryRequest;
import com.example.ecommerce_api.category.dto.response.CategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.CategoryResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryDetailResponse;
import com.example.ecommerce_api.category.entity.Category;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final ProductMapper productMapper;
    private final SubCategoryMapper subCategoryMapper;

    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setImage(request.getImage());
        return category;
    }

    public Category updateEntity(Category category, CategoryRequest request) {
        if (request == null) {
            return category;
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getSlug() != null) {
            category.setSlug(request.getSlug());
        }
        if (request.getImage() != null) {
            category.setImage(request.getImage());
        }
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .image(category.getImage())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public List<CategoryResponse> toResponseList(List<Category> categories) {
        return categories.stream().map(this::toResponse).toList();
    }

    public CategoryDetailResponse toDetailResponse(Category category) {
        return CategoryDetailResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .image(category.getImage())
                .subCategories(category.getSubCategories().stream().map(this::toSubCategoryDetail).toList())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private SubCategoryDetailResponse toSubCategoryDetail(SubCategory subCategory) {
        return SubCategoryDetailResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .build();
    }
}
