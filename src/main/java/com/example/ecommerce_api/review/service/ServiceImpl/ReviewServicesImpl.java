package com.example.ecommerce_api.review.service.ServiceImpl;

import com.example.ecommerce_api.common.exception.DuplicateResourceException;
import com.example.ecommerce_api.common.exception.ForbiddenException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.repository.ProductRepository;
import com.example.ecommerce_api.review.dto.ReviewRequest;
import com.example.ecommerce_api.review.dto.ReviewResponse;
import com.example.ecommerce_api.review.entity.Review;
import com.example.ecommerce_api.review.mapper.ReviewMapper;
import com.example.ecommerce_api.review.repository.ReviewRepository;
import com.example.ecommerce_api.review.service.ReviewServices;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Service
@AllArgsConstructor
@Slf4j
public class ReviewServicesImpl implements ReviewServices {

    private final ReviewRepository reviewRepository;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final ReviewMapper reviewMapper;


    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {

        Page<ReviewResponse> page = reviewRepository.findByProductId(productId,pageable).map(reviewMapper::toResponse);
        log.info("Get ProductReviews for product id {}", productId);

        return new PageResponse<>(page);

    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getUserReviews(Long UserId, Pageable pageable) {

        Page<ReviewResponse> page = reviewRepository.findByUserId(UserId,pageable).map(reviewMapper::toResponse);


        log.info("Get reviews for user {}",UserId);

        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getById(Long id) {

        Review response = reviewRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Review not found by id :"+id)
        );


        log.info("Get review by id  {}", id );
        return reviewMapper.toResponse(response);
    }

    @Override
    public PageResponse<ReviewResponse> getAllReviews(Pageable pageable) {

        Page<ReviewResponse> page = reviewRepository
                .findAll(pageable)
                .map(reviewMapper::toResponse);


        return new PageResponse<>(page);
    }

    @Override
    public ReviewResponse create(Long userId,Long productId, ReviewRequest request) {

        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException(  "You have already reviewed this product");
        }

        if (request.getRatings() == null || request.getRatings().compareTo(BigDecimal.ONE) <0 || request.getRatings().compareTo(BigDecimal.valueOf(5))>0) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5"
            );
        }

        User user =  userRepository.findById(userId).orElseThrow(
                ()-> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );
       Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);



        Review saved = reviewRepository.save(review);
        log.info("Created review {} for product {} by user {}", saved.getId(), productId, userId);

        return reviewMapper.toResponse(saved);
    }

    @Override
    public ReviewResponse update(Long userId,Long reviewId, ReviewRequest request) {


        Review review = reviewRepository.findById(reviewId).orElseThrow(
                ()-> new ResourceNotFoundException("Review not found")
        );

        assertOwner(review,userId);

        if (request.getRatings() ==null || request.getRatings().compareTo(BigDecimal.ONE) <0 || request.getRatings().compareTo(BigDecimal.valueOf(5))>0) {
            throw  new  IllegalArgumentException("Rating must be between 1 and 5");
        }


        reviewMapper.toEntity(request);

        Review saved = reviewRepository.save(review);



        log.info("Update review {}", reviewId);

        return reviewMapper.toResponse(saved);
    }

    @Override
    public void delete(Long reviewId, Long userId) {

        Review review = reviewRepository.findById(reviewId).orElseThrow(
                ()-> new ResourceNotFoundException("Review not found")
        );
        assertOwner(review,userId);
        Product product = review.getProduct();
        reviewRepository.delete(review);
        refreshProductRating(product);

    }

    private void assertOwner(Review review, Long userId) {

        if (!review.getUser().getId().equals(userId))
        {
            throw new ForbiddenException("You are not allowed to modify this review");
        }
    }

    private void  refreshProductRating(Product product)
    {
        ReviewRepository.RatingStats ratingStats = reviewRepository.getRatingStats(product.getId());
        long count    = ratingStats.getCount();
        BigDecimal average = (count ==0 || ratingStats.getAverage() ==null) ?BigDecimal.ZERO: BigDecimal.valueOf(ratingStats.getAverage());

        product.setRatingsAverage(average);
        product.setRatingsQuantity((int) count);
    }
}

