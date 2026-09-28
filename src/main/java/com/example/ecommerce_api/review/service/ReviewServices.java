package com.example.ecommerce_api.review.service;

import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.review.dto.ReviewRequest;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import lombok.extern.java.Log;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ReviewServices {

    PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    PageResponse<ReviewResponse> getUserReviews(Long UserId, Pageable pageable);
    ReviewResponse getById(Long id);

    ReviewResponse create(Long userId,Long productId,
                          ReviewRequest request);
    ReviewResponse update(Long userId, Long reviewId, ReviewRequest request);
    void delete(
            Long reviewId,
            Long userId
    );

    PageResponse<ReviewResponse> getAllReviews(Pageable pageable);
}
