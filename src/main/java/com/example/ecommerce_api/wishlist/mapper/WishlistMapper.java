package com.example.ecommerce_api.wishlist.mapper;

import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.wishlist.dto.response.WishlistItemResponse;
import com.example.ecommerce_api.wishlist.entity.WishlistItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class WishlistMapper {

    public WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();
        BigDecimal priceFrom = product.getVariants() == null ? null : product.getVariants().stream()
                .filter(ProductVariants::isActive)
                .map(this::resolveUnitPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getTitle())
                .slug(product.getSlug())
                .imageUrl(product.imageFor(null))
                .priceFrom(priceFrom)
                .ratingsAverage(product.getRatingsAverage())
                .available(product.isActive() && priceFrom != null)
                .addedAt(item.getCreatedAt())
                .build();
    }

    /** Same rule the cart uses for its unit price. */
    private BigDecimal resolveUnitPrice(ProductVariants variant) {
        return variant.getPriceAfterDiscount() != null
                ? variant.getPriceAfterDiscount()
                : variant.getPrice();
    }
}
