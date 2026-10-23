package com.example.ecommerce_api.cart.service;

import com.example.ecommerce_api.common.exception.BusinessException;
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
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.inventory.repository.InventoryRepository;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
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
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SecurityUtils securityUtils;
    private final CartMapper cartMapper;

    @Override
    @Transactional   // not readOnly: getOrCreateCart may insert a new cart
    public CartResponse getCurrentCart() {

        User user = securityUtils.getCurrentUser();
        return cartMapper.toResponse(getOrCreateCart(user));
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {

        User user = securityUtils.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        ProductVariants variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found: " + request.getProductVariantId()));

        requireAvailable(variant);

        CartItem existing = cartItemRepository.findByCartIdAndProductVariantsId(cart.getId(), variant.getId())
                .orElse(null);
        int newQuantity = (existing != null ? existing.getQuantity() : 0) + request.getQuantity();

        // Validate before attaching a new item: the inventory query triggers an auto-flush,
        // which would otherwise insert a half-built item (price = null) via cascade.
        validateStock(variant, newQuantity);

        CartItem cartItem = existing != null ? existing : new CartItem();
        cartItem.setProductVariants(variant);
        cartItem.setQuantity(newQuantity);
        cartItem.setPrice(resolveUnitPrice(variant));
        if (existing == null) {
            cart.addItem(cartItem);
        }
        cart.recalculateTotalPrice();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getExistingCart();
        CartItem item = findItem(cart, cartItemId);

        requireAvailable(item.getProductVariants());
        validateStock(item.getProductVariants(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        item.setPrice(resolveUnitPrice(item.getProductVariants()));   // refresh to current price
        cart.recalculateTotalPrice();

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


    private Cart getExistingCart()
    {
        User user = securityUtils.getCurrentUser();
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private CartItem findItem(Cart cart, Long cartItemId)
    {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(()-> new ResourceNotFoundException("Cart item not found: " + cartItemId));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            // Not save + catch DataIntegrityViolationException: by the time that's caught, the
            // transaction is already marked rollback-only, so the "refetch" would still fail at commit.
            if (cartRepository.insertIfAbsent(user.getId()) == 0) {
                log.debug("Cart already exists for user {}", user.getId());
            }
            return cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Cart missing right after insert"));
        });
    }


    private void requireAvailable(ProductVariants variant) {
        if (!variant.isActive() || variant.getProduct() == null || !variant.getProduct().isActive()) {
            throw new BusinessException("This product option is no longer available");
        }
    }

    private void validateStock(ProductVariants product, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        int available = inventoryRepository.findByProductVariant_Id(product.getId()).map(Inventory::getAvailableQuantity)
                .orElse(0);
        if (available < quantity) {
            throw new BadRequestException("Insufficient stock. Requested=" + quantity
                    + ", available=" + available);
        }
    }

    /** Same rule OrderItem uses, so cart total = order subtotal. */
    private BigDecimal resolveUnitPrice(ProductVariants variant) {
        return variant.getPriceAfterDiscount() != null
                ? variant.getPriceAfterDiscount()
                : variant.getPrice();
    }
}
