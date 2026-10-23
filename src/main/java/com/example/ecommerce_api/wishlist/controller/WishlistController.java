package com.example.ecommerce_api.wishlist.controller;

import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.wishlist.dto.response.WishlistItemResponse;
import com.example.ecommerce_api.wishlist.dto.response.WishlistStatusResponse;
import com.example.ecommerce_api.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "List the current user's wishlist", description = "Newest first, paged")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WishlistItemResponse>>> getMyWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(page, size)));
    }

    @Operation(summary = "Number of products in the current user's wishlist", description = "For the header badge")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countMyWishlist() {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.countMyWishlist()));
    }

    @Operation(summary = "Ids of every product in the current user's wishlist",
            description = "Newest first, unpaged; lets product listings mark saved items")
    @GetMapping("/product-ids")
    public ResponseEntity<ApiResponse<List<Long>>> getMyProductIds() {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyProductIds()));
    }

    @Operation(summary = "Check whether a product is in the current user's wishlist", description = "Drives the heart icon on product pages")
    @GetMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<WishlistStatusResponse>> getStatus(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getStatus(productId)));
    }

    @Operation(summary = "Add a product to the wishlist", description = "Idempotent: re-adding returns the existing entry")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product is in the wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product is no longer available"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.addToWishlist(productId), "Added to wishlist"));
    }

    @Operation(summary = "Remove a product from the wishlist")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not in wishlist")
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove all products from the wishlist")
    @DeleteMapping
    public ResponseEntity<Void> clearWishlist() {
        wishlistService.clearWishlist();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Move a wishlisted product to the cart",
            description = "Adds the chosen variant (size/color) to the cart and removes the product from the wishlist")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moved; returns the updated cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Variant doesn't belong to the product, or insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product option is no longer available"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not in wishlist or variant not found")
    })
    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<ApiResponse<CartResponse>> moveToCart(@PathVariable Long productId,
                                                                @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.moveToCart(productId, request), "Moved to cart"));
    }
}
