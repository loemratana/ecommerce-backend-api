package com.example.ecommerce_api.product.mapper;


import com.example.ecommerce_api.product.dto.response.ProductSizeResponse;
import com.example.ecommerce_api.product.dto.response.ProductVariantResponse;
import com.example.ecommerce_api.product.dto.request.ProductVariantsRequest;
import com.example.ecommerce_api.product.entity.ProductSize;
import com.example.ecommerce_api.product.entity.ProductVariants;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantMapper {

    public ProductVariants toEntity(ProductVariantsRequest request) {
        ProductVariants productVariants = new ProductVariants();
        productVariants.setColor(request.getColor());
        productVariants.setSku(request.getSku());
        productVariants.setPrice(request.getPrice());
        productVariants.setPriceAfterDiscount(request.getPriceAfterDiscount());
        productVariants.setSoldQuantity(request.getSoldQuantity()  != null ? request.getSoldQuantity() : 0);
        return productVariants;
    }

    public ProductVariantResponse toResponse(ProductVariants productVariants)
    {
        return ProductVariantResponse.builder()
                .id(productVariants.getId())
                .color(productVariants.getColor())
                .sku(productVariants.getSku())
                .price(productVariants.getPrice())
                .priceAfterDiscount(productVariants.getPriceAfterDiscount())
                .soldQuantity(productVariants.getSoldQuantity())
                .size(toSizeResponse(productVariants.getSize()))
                .build();
    }

    private ProductSizeResponse toSizeResponse(ProductSize size) {
        if (size == null) {
            return null;
        }
        return ProductSizeResponse.builder()
                .id(size.getId())
                .name(size.getName())
                .build();
    }

    public void updateEntity(ProductVariantsRequest request, ProductVariants entity) {
        entity.setColor(request.getColor());
        entity.setSku(request.getSku());
        entity.setPrice(request.getPrice());
        entity.setPriceAfterDiscount(request.getPriceAfterDiscount());
        entity.setSoldQuantity(request.getSoldQuantity() != null ? request.getSoldQuantity() : 0);
    }
}
