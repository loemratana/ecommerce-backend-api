package com.example.ecommerce_api.wishlist.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class WishlistItemResponse {
    private Long id;

    private Long productId;
    private String productName;
    private String slug;
    private String imageUrl;

    /** Lowest current price among the product's active variants; null if it has none. */
    private BigDecimal priceFrom;
    private BigDecimal ratingsAverage;

    /** false = product was archived or has no active option left; it can't be moved to the cart. */
    private boolean available;

    private LocalDateTime addedAt;
}
