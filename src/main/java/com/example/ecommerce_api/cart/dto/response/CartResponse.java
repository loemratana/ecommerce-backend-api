package com.example.ecommerce_api.cart.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {

   private Long cartId;

    private Long userId;

    private BigDecimal totalCartPrice;

    private BigDecimal totalPriceAfterDiscount;

    private List<CartItemResponse> items;

}
