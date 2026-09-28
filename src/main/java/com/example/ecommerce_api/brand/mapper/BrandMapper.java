package com.example.ecommerce_api.brand.mapper;


import com.example.ecommerce_api.brand.dto.request.BrandRequest;
import com.example.ecommerce_api.brand.dto.response.BrandResponse;
import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BrandMapper {

    private final ProductMapper productMapper;

    public Brand toEntity(BrandRequest request) {
        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setSlug(request.getSlug());
        brand.setImage(request.getImage());
        return brand;
    }

    public Brand updateEntity(Brand brand, BrandRequest request) {
        if (request == null) {
            return brand;
        }

        if (request.getName() != null) {
            brand.setName(request.getName());
        }
        if (request.getSlug() != null) {
            brand.setSlug(request.getSlug());
        }
        if (request.getImage() != null) {
            brand.setImage(request.getImage());
        }
        return brand;
    }

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .image(brand.getImage())
                .products(productMapper.toProductSummary(brand.getProducts()))
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    public List<BrandResponse> toResponseList(List<Brand> brands) {
        return brands.stream().map(this::toResponse).toList();
    }
}
