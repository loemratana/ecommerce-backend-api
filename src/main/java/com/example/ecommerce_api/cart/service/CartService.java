package com.example.ecommerce_api.cart.service;

import com.example.ecommerce_api.cart.dto.request.AddCartItemRequest;
import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.dto.request.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCurrentCart();

    CartResponse addToCart(AddToCartRequest request);

    CartResponse updateCartItem(
            Long cartItemId,
            UpdateCartItemRequest request
    );

    void removeCartItem(Long cartItemId);

    void clearCart();

}
