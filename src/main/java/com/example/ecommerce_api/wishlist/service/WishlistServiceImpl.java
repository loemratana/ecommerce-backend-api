package com.example.ecommerce_api.wishlist.service;

import com.example.ecommerce_api.cart.dto.request.AddToCartRequest;
import com.example.ecommerce_api.cart.dto.response.CartResponse;
import com.example.ecommerce_api.cart.service.CartService;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.wishlist.dto.response.WishlistItemResponse;
import com.example.ecommerce_api.wishlist.dto.response.WishlistStatusResponse;
import com.example.ecommerce_api.wishlist.mapper.WishlistMapper;
import com.example.ecommerce_api.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private static final int MAX_PAGE_SIZE = 50;

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartService cartService;
    private final SecurityUtils securityUtils;
    private final WishlistMapper wishlistMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> getMyWishlist(int page, int size) {
        Long userId = securityUtils.getCurrentUserId();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));

        return new PageResponse<>(wishlistItemRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(wishlistMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyWishlist() {
        return wishlistItemRepository.countByUserId(securityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyProductIds() {
        return wishlistItemRepository.findProductIdsByUserId(securityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatusResponse getStatus(Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        return new WishlistStatusResponse(productId,
                wishlistItemRepository.existsByUserIdAndProductId(userId, productId));
    }

    /** Idempotent: adding a product that's already wishlisted just returns the existing entry. */
    @Override
    @Transactional
    public WishlistItemResponse addToWishlist(Long productId) {
        Long userId = securityUtils.getCurrentUserId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new BusinessException("This product is no longer available");
        }

        if (wishlistItemRepository.insertIfAbsent(userId, productId) == 0) {
            log.debug("Product {} already in wishlist of user {}", productId, userId);
        }
        return wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .map(wishlistMapper::toResponse)
                .orElseThrow(() -> new IllegalStateException("Wishlist item missing right after insert"));
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        if (wishlistItemRepository.deleteByUserIdAndProductId(userId, productId) == 0) {
            throw new ResourceNotFoundException("Product not in wishlist: " + productId);
        }
    }

    @Override
    @Transactional
    public void clearWishlist() {
        wishlistItemRepository.deleteAllByUserId(securityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public CartResponse moveToCart(Long productId, AddToCartRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        if (!wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not in wishlist: " + productId);
        }

        ProductVariants variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found: " + request.getProductVariantId()));
        if (variant.getProduct() == null || !productId.equals(variant.getProduct().getId())) {
            throw new BadRequestException("Variant " + variant.getId() + " does not belong to product " + productId);
        }

        // Cart does the availability + stock checks; if it throws, the wishlist entry is kept.
        CartResponse cart = cartService.addToCart(request);
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
        return cart;
    }
}
