package com.example.ecommerce_api.cart.mapper;


import com.example.ecommerce_api.cart.dto.response.CartItemResponse;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.entity.CartItem;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() == null
                ? Collections.emptyList()
                : cart.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList());

        return new CartResponse(
                cart.getId(),
                cart.getUser() != null ? cart.getUser().getId() : null,
                cart.getTotalCartPrice(),
                cart.getTotalPriceAfterDiscount(),
                itemResponses
        );
    }

    private CartItemResponse toItemResponse(CartItem item)
    {
        ProductVariants variant = item.getProductVariants();
        Product product = variant !=null ? variant.getProduct() :null;

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product !=null? product.getId():null)
                .productName(product !=null ?product.getTitle() :null)
                .productVariantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .color(variant != null ? variant.getColor() : null)
                .sizeName(variant != null && variant.getSize() != null ? variant.getSize().getName() : null)
                .imageUrl(product != null ? product.imageFor(variant.getColor()) : null)
                .available(variant != null && variant.isActive() && product != null && product.isActive())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .lineTotal(item.getSubtotal())
                .build();
    }


}
