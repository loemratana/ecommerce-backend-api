package com.example.ecommerce_api.review.repository;

import com.example.ecommerce_api.review.entity.Review;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByProduct_Id(Long productId);

    interface  RatingStats{
        Double getAverage();
        Long getCount();
    }

    Page<Review> findAll(Pageable pageable);




    @EntityGraph(attributePaths = {"user","product"})
    Page<Review> findByProductId(Long productId, Pageable pageable);


    @EntityGraph(attributePaths = {"user","product"})
    Page<Review> findByUserId(Long userId,Pageable pageable);


    @Override
    @EntityGraph(attributePaths = {"user","product"})
    Optional<Review> findById(Long id);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);


    @Query("select  avg(r.ratings) as average,count(r) as count from Review  r where r.product.id =:productId")
    RatingStats getRatingStats(@Param("productId") Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
