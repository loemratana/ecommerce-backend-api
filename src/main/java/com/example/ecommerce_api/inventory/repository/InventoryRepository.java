package com.example.ecommerce_api.inventory.repository;

import com.example.ecommerce_api.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.size"})
    Optional<Inventory> findByProductVariant_Id(Long productVariantId);

    @Override
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.size"})
    Page<Inventory> findAll(Pageable pageable);

    boolean existsByProductVariant_Id(Long productVariantId);

    /**
     * Filtered list for the admin table. search = '' disables the text filter;
     * minAvailable/maxAvailable bound the available stock (quantity - reserved).
     */
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.size"})
    @Query(value = "select i from Inventory i join i.productVariant v join v.product p " +
            "where (:search = '' or lower(p.title) like concat('%', :search, '%') " +
            "       or lower(v.sku) like concat('%', :search, '%') " +
            "       or lower(coalesce(v.color, '')) like concat('%', :search, '%')) " +
            "and (i.quantity - i.reservedQuantity) between :minAvailable and :maxAvailable",
            countQuery = "select count(i) from Inventory i join i.productVariant v join v.product p " +
                    "where (:search = '' or lower(p.title) like concat('%', :search, '%') " +
                    "       or lower(v.sku) like concat('%', :search, '%') " +
                    "       or lower(coalesce(v.color, '')) like concat('%', :search, '%')) " +
                    "and (i.quantity - i.reservedQuantity) between :minAvailable and :maxAvailable")
    Page<Inventory> search(@Param("search") String search,
                           @Param("minAvailable") int minAvailable,
                           @Param("maxAvailable") int maxAvailable,
                           Pageable pageable);

    @Query("select count(i) from Inventory i where (i.quantity - i.reservedQuantity) between :min and :max")
    long countByAvailableBetween(@Param("min") int min, @Param("max") int max);

    @Query("select coalesce(sum(i.quantity), 0) from Inventory i")
    long sumQuantity();

    @Query("select coalesce(sum(i.reservedQuantity), 0) from Inventory i")
    long sumReservedQuantity();

    /** Records whose available stock (quantity - reserved) is at or below the threshold. */
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product", "productVariant.size"})
    @Query("select i from Inventory i where (i.quantity - i.reservedQuantity) <= :threshold")
    Page<Inventory> findLowStock(@Param("threshold") int threshold, Pageable pageable);

    /**
     * Row-locks the inventory record for the duration of the transaction, so concurrent
     * reserve/release/sale calls for the same variant serialize instead of racing on
     * quantity/reservedQuantity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productVariant.id = :productVariantId")
    Optional<Inventory> lockByProductVariant_Id(@Param("productVariantId") Long productVariantId);
}
