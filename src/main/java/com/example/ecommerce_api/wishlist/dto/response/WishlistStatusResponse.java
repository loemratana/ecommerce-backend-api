package com.example.ecommerce_api.wishlist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WishlistStatusResponse {

    private Long productId;

    private boolean inWishlist;
}
