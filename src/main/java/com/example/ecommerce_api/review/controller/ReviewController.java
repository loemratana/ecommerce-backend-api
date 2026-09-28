package com.example.ecommerce_api.review.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.review.dto.ReviewRequest;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import com.example.ecommerce_api.review.repository.ReviewRepository;
import com.example.ecommerce_api.review.service.ReviewServices;
import com.example.ecommerce_api.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewServices reviewServices;




    @Operation(description = "Get All review from customer")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(@PageableDefault(size = 10) Pageable pageable) {

        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return ResponseEntity.ok().body(ApiResponse.success(reviewServices.getAllReviews(safePageable)));
    }

    @Operation(description = "Create a review for a product")
    @PostMapping("product/{productId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview( @AuthenticationPrincipal UserDetailsImpl principal,@PathVariable Long productId, @Valid  @RequestBody ReviewRequest request )
    {

        ReviewResponse response = reviewServices.create(principal.getId(),productId,request);

        return ResponseEntity.ok().body(ApiResponse.success(response,"Review Created Successfully"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewServices.getProductReviews(productId, pageable)));
    }
    @Operation(description = "List reviews written by the authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewServices.getUserReviews(principal.getId(), pageable)));
    }

    @Operation(description = "Update own review")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewServices.update(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }



    @Operation(description = "Delete own review")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id) {
        reviewServices.delete(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }


}
