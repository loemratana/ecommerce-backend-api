package com.example.ecommerce_api.wishlist.repository;

import com.example.ecommerce_api.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    // Product's variants/images are @BatchSize'd, so the mapper doesn't N+1 over the page.
    // id breaks createdAt ties, so items don't swap between pages.
    @EntityGraph(attributePaths = "product")
    Page<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("""
select w.product.id from WishlistItem w where w.user.id = :userId order by w.createdAt desc, w.id desc
""")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    /** Atomic add: a concurrent duplicate (e.g. a double-click) is a no-op instead of a constraint error. */
    @Modifying
    @Query(value = """
insert into wishlist_items (user_id, product_id, created_at) values (:userId, :productId, now())
on conflict (user_id, product_id) do nothing
""", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("productId") Long productId);

    @Modifying
    @Query("""
delete from WishlistItem w where w.user.id = :userId and w.product.id = :productId
""")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Modifying
    @Query("""
delete from WishlistItem w where w.user.id = :userId
""")
    void deleteAllByUserId(@Param("userId") Long userId);
}
