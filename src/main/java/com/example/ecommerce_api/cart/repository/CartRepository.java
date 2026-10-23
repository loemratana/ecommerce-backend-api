package com.example.ecommerce_api.cart.repository;

import com.example.ecommerce_api.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);


    @Query(
            """
                select distinct  c from  Cart  c 
                            left  join fetch c.items i
                            left  join fetch i.productVariants v 
                            left  join fetch v.product 
                            where c.user.id =:userId
            """
    )
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    /** Atomic create: a concurrent first request for the same user is a no-op instead of a constraint error. */
    @Modifying
    @Query(value = """
insert into carts (user_id, total_cart_price, total_price_after_discount, version, created_at, updated_at)
values (:userId, 0, 0, 0, now(), now())
on conflict (user_id) do nothing
""", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId);
}
