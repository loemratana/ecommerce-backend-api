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
        productVariants.setColor(normalizeColor(request.getColor()));
        productVariants.setSku(request.getSku());
        productVariants.setPrice(request.getPrice());
        productVariants.setSoldQuantity(0);   // only the order flow (confirmSale) changes it
        return productVariants;
    }

    public ProductVariantResponse toResponse(ProductVariants productVariants)
    {
        return ProductVariantResponse.builder()
                .id(productVariants.getId())
                .status(productVariants.getStatus())
                .color(productVariants.getColor())
                .sku(productVariants.getSku())
                .price(productVariants.getPrice())
                // kept in the response for the frontends: both are the product's discount now
                .discountPercent(productVariants.getEffectiveDiscountPercent())
                .effectiveDiscountPercent(productVariants.getEffectiveDiscountPercent())
                .priceAfterDiscount(productVariants.getPriceAfterDiscount())
                .soldQuantity(productVariants.getSoldQuantity())
                .discountAmount(productVariants.getDiscountAmount())
                .availableQuantity(productVariants.getInventory() != null
                        ? productVariants.getInventory().getAvailableQuantity() : 0)
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
        entity.setColor(normalizeColor(request.getColor()));
        entity.setSku(request.getSku());
        entity.setPrice(request.getPrice());
    }

    private String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.trim();
    }
}
