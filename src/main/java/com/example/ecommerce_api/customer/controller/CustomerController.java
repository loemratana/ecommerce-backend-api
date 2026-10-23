package com.example.ecommerce_api.customer.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.customer.dto.CustomerDetailResponse;
import com.example.ecommerce_api.customer.dto.CustomerStatsResponse;
import com.example.ecommerce_api.customer.dto.CustomerSummaryResponse;
import com.example.ecommerce_api.customer.service.CustomerService;
import com.example.ecommerce_api.order.dto.OrderSummaryResponse;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "customer", description = "Admin view of customer accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "List customers with order count, total spent and last order date",
            description = "Sortable by createdAt, firstName, lastName, email or active")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerSummaryResponse>>> getAll(
            @Parameter(description = "Matches name, email or phone") @RequestParam(required = false) String search,
            @Parameter(description = "true = active only, false = inactive only, omit for both") @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAll(search, active, pageable)));
    }

    @Operation(summary = "Customer counts for the admin dashboard")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CustomerStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getStats()));
    }

    @Operation(summary = "Customer profile with addresses and order totals")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
    }

    @Operation(summary = "A customer's orders, newest first")
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getOrders(id, pageable)));
    }

    @Operation(summary = "A customer's product reviews")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @PathVariable Long id,
            @PageableDefault(size = 10) Pageable pageable) {
        // Same as GET /review: sorting is not exposed for reviews.
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(ApiResponse.success(customerService.getReviews(id, unsorted)));
    }

    @Operation(summary = "Re-enable a customer account")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.setActive(id, true), "Customer activated"));
    }

    @Operation(summary = "Disable a customer account")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.setActive(id, false), "Customer deactivated"));
    }
}
