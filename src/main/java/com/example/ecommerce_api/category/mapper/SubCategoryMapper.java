package com.example.ecommerce_api.category.mapper;


import com.example.ecommerce_api.category.dto.request.SubCategoryRequest;
import com.example.ecommerce_api.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryDetailResponse;
import com.example.ecommerce_api.category.dto.response.SubCategoryResponse;
import com.example.ecommerce_api.category.entity.Category;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import com.example.ecommerce_api.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubCategoryMapper {

    public SubCategory toEntity(SubCategoryRequest request) {
        SubCategory subCategory = new SubCategory();
        subCategory.setName(request.getName());
        subCategory.setSlug(request.getSlug());
        return subCategory;
    }

    public SubCategory updateEntity(SubCategory subCategory, SubCategoryRequest request) {
        if (request == null) {
            return subCategory;
        }

        if (request.getName() != null) {
            subCategory.setName(request.getName());
        }
        if (request.getSlug() != null) {
            subCategory.setSlug(request.getSlug());
        }
        return subCategory;
    }

    public SubCategoryDetailResponse toDetailResponse(
            SubCategory subCategory
    ) {

        Category category = subCategory.getCategory();

        SubCategoryDetailResponse response =
                new SubCategoryDetailResponse();

        response.setId(subCategory.getId());
        response.setName(subCategory.getName());
        response.setSlug(subCategory.getSlug());

        if (category != null) {
            response.setCategory(
                    toCategorySummary(category)
            );
        }

        if (subCategory.getProducts() != null) {
            response.setProducts(
                    subCategory.getProducts()
                            .stream()
                            .map(this::toProductSummary)
                            .toList()
            );
        }

        response.setCreatedAt(subCategory.getCreatedAt());
        response.setUpdatedAt(subCategory.getUpdatedAt());

        return response;
    }

    public CategorySummaryResponse toCategorySummary(Category category) {
        return CategorySummaryResponse.builder()
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    public ProductSummaryResponse toProductSummary(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getTitle())
                .slug(product.getSlug())
                .imageUrl(product.getImageCover())
                .build();
    }


    public SubCategoryResponse toResponse(SubCategory subCategory) {
        return SubCategoryResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .slug(subCategory.getSlug())
                .categoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .categoryName(subCategory.getCategory() != null ? subCategory.getCategory().getName() : null)
                .createdAt(subCategory.getCreatedAt())
                .updatedAt(subCategory.getUpdatedAt())
                .build();
    }

    public List<SubCategoryResponse> toResponseList(List<SubCategory> subCategories) {
        return subCategories.stream().map(this::toResponse).toList();
    }
}
