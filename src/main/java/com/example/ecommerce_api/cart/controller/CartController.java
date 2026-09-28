package com.example.ecommerce_api.cart.controller;


import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.request.UpdateCartItemRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get the current user's cart", description = "Returns the authenticated user's cart, or 204 if they don't have one yet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart found",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "204", description = "User has no cart yet")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCurrentCart() {
        CartResponse response = cartService.getCurrentCart();
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a product to the cart", description = "Adds the given quantity, or increases it if the product is already in the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Product not sellable or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addToCart(request));
    }

    @Operation(summary = "Update a cart item's quantity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quantity updated",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable Long cartItemId,
                                                         @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(cartItemId, request));
    }

    @Operation(summary = "Remove a single item from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removed"),
            @ApiResponse(responseCode = "404", description = "Cart or cart item not found")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove all items from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart cleared"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
