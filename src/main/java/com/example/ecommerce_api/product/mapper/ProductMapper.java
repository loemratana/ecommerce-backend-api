package com.example.ecommerce_api.product.mapper;


import com.example.ecommerce_api.brand.dto.request.BrandRequest;
import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.category.dto.request.SubCategoryRequest;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.product.dto.request.ProductRequest;
import com.example.ecommerce_api.product.dto.response.*;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductImage;
import com.example.ecommerce_api.product.entity.ProductVariants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductImageMapper productImageMapper;

    private final ProductVariantMapper productVariantMapper;


    public Product toEntity(ProductRequest productRequest) {

        Product product = new Product();
        product.setTitle(productRequest.getTitle());
        product.setSlug(productRequest.getSlug());
        product.setDescription(productRequest.getDescription());
        product.setImageCover(productRequest.getImageCover());

        return product;

    }

    public List<ProductSummaryResponse> toProductSummary(List<Product> products) {

        return  products.stream().map(
                product -> ProductSummaryResponse.builder()
                        .id(product.getId())
                        .name(product.getTitle())
                        .slug(product.getSlug())
                        .imageUrl(product.getImageCover())
                        .build()).toList();

    }

    public ProductSummaryResponse toProductSummary(Product product) {

        if (product == null) {
            return null;
        }

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getTitle())
                .slug(product.getSlug())
                .imageUrl(product.getImageCover())
                .build();
    }

    public ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .slug(product.getSlug())
                .description(product.getDescription())
                .imageCover(product.getImageCover())
                .ratingsAverage(product.getRatingsAverage())
                .ratingsQuantity(product.getRatingsQuantity())
                .subCategory(toSubCategory(product.getSubCategory()))
                .brand(toBrand(product.getBrand()))
                .variants(mapToVariant(product.getVariants()))
                .variants(mapToVariant(product.getVariants()))
                .variants(mapToVariant(product.getVariants()))
                .variants(mapToVariant(product.getVariants()))
                .variants(mapToVariant(product.getVariants()))
                .variants(mapToVariant(product.getVariants()))
                .images(toImageResponses(product.getImages()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();


    }



    private List<ProductVariantResponse> mapToVariant(List<ProductVariants> productVariants) {
        return productVariants.stream().map(productVariantMapper::toResponse).toList();
    }
    public List<ProductResponse> toResponse(List<Product> products) {
        return products.stream().map(this::toResponse).toList();
    }


    private SubCategoryResponse toSubCategory(SubCategory category) {
        return SubCategoryResponse.builder()
                .subCategoryId(category.getId())
                .subCategoryName(category.getName())
                .build();
    }

    private BrandResponse toBrand(Brand brand)
    {
        return BrandResponse.builder()
                .brandId(brand.getId())
                .brandName(brand.getName())
                .build();
    }

    private List<ProductImageResponse> toImageResponses(List<ProductImage> images) {
        if (images == null) {
            return Collections.emptyList();
        }
        return images.stream().map(productImageMapper::toResponse).toList();
    }

}
