package com.example.ecommerce_api.cart.service;

import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.request.UpdateCartItemRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.entity.CartItem;
import com.example.ecommerce_api.cart.mapper.CartMapper;
import com.example.ecommerce_api.cart.repository.CartItemRepository;
import com.example.ecommerce_api.cart.repository.CartRepository;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCurrentCart() {

        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {

        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException(
                "Product not found: " + request.getProductId()));

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() ->
                        {
                            CartItem fresh = new CartItem();
                            fresh.setProduct(product);
                            fresh.setQuantity(0);
                            cart.addItem(fresh);
                            return fresh;
                        }
                );
        int newQuantity = cartItem.getQuantity() + request.getQuantity();
        validateStock(product, newQuantity);
        cartItem.setQuantity(newQuantity);
        cartItem.setPrice(resolveUnitPrice(product));
        calculateCartTotal(cart);


        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        User user = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));


        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found: " + cartItemId));

        Product product = item.getProduct();
        validateStock(product, request.getQuantity());

        item.setQuantity(request.getQuantity());
        calculateCartTotal(cart);

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId) {
        User user = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found: " + cartItemId));

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart() {
        User user = securityUtils.getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cartItemRepository.deleteAllByCartId(cart.getId());
    }

    // ---------- HELPERS ----------

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            try {
                return cartRepository.save(cart);
            } catch (DataIntegrityViolationException ex) {
                // Race: another request created it — refetch

                log.debug("Cart already exists for user {}", user.getId());
                return cartRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Cart creation race condition"
                        ));
            }
        });
    }


    private void validateStock(Product product, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock. Requested=" + quantity
                    + ", available=" + product.getStockQuantity());

        }
    }

    private BigDecimal resolveUnitPrice(Product product) {
        return product.getVariants().stream()
                .map(ProductVariants::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new BadRequestException(
                        "Product has no priced variants: " + product.getId()));
    }

    private void calculateCartTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalCartPrice(total);

        cart.setTotalPriceAfterDiscount(total);
    }
}
