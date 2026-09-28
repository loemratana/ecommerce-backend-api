package com.example.ecommerce_api.inventory.repository;

import com.example.ecommerce_api.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findAllByInventory_IdOrderByCreatedAtDesc(Long inventoryId);
}
