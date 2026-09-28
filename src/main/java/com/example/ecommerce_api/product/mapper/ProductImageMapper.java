package com.example.ecommerce_api.product.mapper;


import com.example.ecommerce_api.product.dto.request.ProductImageRequest;
import com.example.ecommerce_api.product.dto.response.ProductImageResponse;
import com.example.ecommerce_api.product.entity.ProductImage;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMapper {

    public ProductImage toEntity(ProductImageRequest request) {
        ProductImage productImage = new ProductImage();
        productImage.setImageUrl(request.getImageUrl());
        productImage.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        return productImage;
    }

    public ProductImageResponse toResponse(ProductImage productImage) {
        return ProductImageResponse.builder()
                .id(productImage.getId())
                .imageUrl(productImage.getImageUrl())
                .sortOrder(productImage.getSortOrder())
                .build();
    }

    public void updateEntity(ProductImageRequest request, ProductImage entity) {
        entity.setImageUrl(request.getImageUrl());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }
}
