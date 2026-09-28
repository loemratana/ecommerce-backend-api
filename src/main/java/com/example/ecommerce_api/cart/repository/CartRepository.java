package com.example.ecommerce_api.cart.repository;

import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);


    @Query(
            """
                select distinct  c from  Cart  c left  join fetch c.items where c.user.id = :userId
            """
    )
    Optional<Cart> findByUserIdWithItems(Long userId);

    Long id(Long id);
}
