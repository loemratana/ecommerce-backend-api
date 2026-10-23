package com.example.ecommerce_api.wishlist.service;

import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.wishlist.dto.response.WishlistItemResponse;
import com.example.ecommerce_api.wishlist.dto.response.WishlistStatusResponse;

import java.util.List;

public interface WishlistService {

    PageResponse<WishlistItemResponse> getMyWishlist(int page, int size);

    long countMyWishlist();

    /** Ids of every wishlisted product, newest first — lets the storefront mark hearts without paging. */
    List<Long> getMyProductIds();

    WishlistStatusResponse getStatus(Long productId);

    WishlistItemResponse addToWishlist(Long productId);

    void removeFromWishlist(Long productId);

    void clearWishlist();

    /** Adds the chosen variant of a wishlisted product to the cart, then drops it from the wishlist. */
    CartResponse moveToCart(Long productId, AddToCartRequest request);
}
