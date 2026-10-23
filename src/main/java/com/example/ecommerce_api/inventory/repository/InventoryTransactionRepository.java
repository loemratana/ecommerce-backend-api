package com.example.ecommerce_api.inventory.repository;

import com.example.ecommerce_api.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findAllByInventory_IdOrderByCreatedAtDesc(Long inventoryId);

    List<InventoryTransaction> findTop10ByInventory_IdOrderByCreatedAtDesc(Long inventoryId);

    Page<InventoryTransaction> findAllByInventory_Id(Long inventoryId, Pageable pageable);

    @Query("select coalesce(sum(t.quantityChange), 0) from InventoryTransaction t " +
            "where t.inventory.id = :inventoryId and t.quantityChange > 0")
    Integer sumStockIn(@Param("inventoryId") Long inventoryId);

    @Query("select coalesce(sum(-t.quantityChange), 0) from InventoryTransaction t " +
            "where t.inventory.id = :inventoryId and t.quantityChange < 0")
    Integer sumStockOut(@Param("inventoryId") Long inventoryId);
}
