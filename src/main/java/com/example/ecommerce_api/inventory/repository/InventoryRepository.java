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
     * Row-locks the inventory record for the duration of the transaction, so concurrent
     * reserve/release/sale calls for the same variant serialize instead of racing on
     * quantity/reservedQuantity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productVariant.id = :productVariantId")
    Optional<Inventory> lockByProductVariant_Id(@Param("productVariantId") Long productVariantId);
}
