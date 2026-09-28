package com.example.ecommerce_api.cart.repository;

import com.example.ecommerce_api.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    List<CartItem> findAllByCartId(Long cartId);

    Optional<CartItem> findByIdAndCartId(Long cartItemId ,Long CartId);


    @Modifying
    @Query("""
delete  from CartItem ci where  ci.cart.id =:cartId
""")
    void deleteAllByCartId(@Param("cartId") Long cartId);

}
