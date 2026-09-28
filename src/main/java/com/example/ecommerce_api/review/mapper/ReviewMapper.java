package com.example.ecommerce_api.review.mapper;


import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.review.dto.ReviewRequest;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import com.example.ecommerce_api.review.dto.ReviewerResponse;
import com.example.ecommerce_api.review.entity.Review;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequest reviewRequest) {
        Review review = new Review();
        review.setTitle(reviewRequest.getTitle());
        review.setRatings(reviewRequest.getRatings());
        review.setComment(reviewRequest.getComment());

        return  review;

    }

    public ReviewResponse toResponse(Review review){

        if (review == null){
            return null;
        }
        return ReviewResponse.builder()
                .id(review.getId())
                .title(review.getTitle())
                .ratings(review.getRatings())
                .comment(review.getComment())
                .user(toReviewer(review.getUser()))
                .product(toProductResponse(review.getProduct()))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();

    }
    private ReviewerResponse toReviewer(User user) {
        return ReviewerResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImage(user.getProfileImage())
                .build();
    }
    public void updateEntity(Review review, ReviewRequest request) {
        review.setTitle(request.getTitle());
        review.setRatings(request.getRatings());
        review.setComment(request.getComment());
    }

    public UserResponse toUserResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .build();

    }

    public ProductSummaryResponse toProductResponse(Product product){
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .imageUrl(product.getImageCover())
                .slug(product.getSlug())
                .build();
    }
}
