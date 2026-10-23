package com.example.ecommerce_api.customer.service;

import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.customer.dto.CustomerDetailResponse;
import com.example.ecommerce_api.customer.dto.CustomerStatsResponse;
import com.example.ecommerce_api.customer.dto.CustomerSummaryResponse;
import com.example.ecommerce_api.order.dto.OrderSummaryResponse;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

/** Admin view of users who hold the CUSTOMER role. */
public interface CustomerService {

    /** {@code search} matches name, email or phone; {@code active} null means both. */
    PageResponse<CustomerSummaryResponse> getAll(String search, Boolean active, Pageable pageable);

    CustomerStatsResponse getStats();

    CustomerDetailResponse getById(Long id);

    PageResponse<OrderSummaryResponse> getOrders(Long id, Pageable pageable);

    PageResponse<ReviewResponse> getReviews(Long id, Pageable pageable);

    CustomerDetailResponse setActive(Long id, boolean active);
}
