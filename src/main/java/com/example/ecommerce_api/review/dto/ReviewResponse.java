package com.example.ecommerce_api.review.dto;


import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private String title;
    private BigDecimal ratings;
    private String comment;

    private ReviewerResponse user;
    private ProductSummaryResponse product;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
