package com.example.ecommerce_api.coupon.repository;

import com.example.ecommerce_api.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);


    @Query(
            """
SELECT c 
FROM  Coupon  c where lower(c.code) =lower(:code)
AND c.active = true
AND c.expiryDate>:now
AND (c.usageLimit IS NULL or c.usedCount < c.usageLimit)
"""
    )
    Optional<Coupon> findValidCoupon(@Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * Atomically increments used_count.
     *
     * This prevents:
     * usageLimit = 100
     * usedCount  = 100
     *
     * from becoming 101 because of concurrent requests.
     */



    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE coupons
        SET used_count = used_count + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
          AND active = true
          AND expiry_date > CURRENT_TIMESTAMP
          AND (
              usage_limit IS NULL
              OR used_count < usage_limit
          )
        """, nativeQuery = true)
    int incrementUsedCount(@Param("id") Long id);

    /** Gives one use back (e.g. the order that used it was cancelled); never goes below 0. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
        UPDATE coupons
        SET used_count = used_count - 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
          AND used_count > 0
        """, nativeQuery = true)
    int decrementUsedCount(@Param("id") Long id);

}
