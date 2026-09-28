package com.example.ecommerce_api.cart.mapper;


import com.example.ecommerce_api.cart.dto.response.CartItemResponse;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.entity.CartItem;
import com.example.ecommerce_api.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public Cart toEntity(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cart;
    }

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

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal price = item.getPrice();
        Integer quantity = item.getQuantity();
        BigDecimal lineTotal = (price != null && quantity != null)
                ? price.multiply(BigDecimal.valueOf(quantity))
                : null;

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getTitle() : null)
                .quantity(quantity)
                .price(price)
                .lineTotal(lineTotal)
                .build();
    }


}
