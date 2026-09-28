package com.example.ecommerce_api.product.mapper;


import com.example.ecommerce_api.product.dto.request.ProductSizeRequest;
import com.example.ecommerce_api.product.dto.request.ProductSizeUpdateRequest;
import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;
import com.example.ecommerce_api.product.entity.ProductSize;
import org.springframework.stereotype.Component;

import java.util.IllegalFormatCodePointException;

@Component
public class ProductSizeMapper {

    public ProductSize toEntity(ProductSizeRequest request) {

        ProductSize productSize = new ProductSize();
        productSize.setName(request.getName().toUpperCase());
        productSize.setDescription(request.getDescription());


        return productSize;

    }

    public ProductSize toUpdatedEntity(ProductSize productSize, ProductSizeUpdateRequest request) {

        if (request.getName() != null) {
            productSize.setName(request.getName().trim().toUpperCase());
        }
        if (request.getDescription() != null)
        {
            productSize.setDescription(request.getDescription());
        }

        return productSize;
    }

    public ProductSizeResponse toResponse(ProductSize productSize) {


        return ProductSizeResponse.builder()
                .id(productSize.getId())
                .name(productSize.getName())
                .description(productSize.getDescription())
                .active(productSize.isStatus())
                .build();
    }
}
