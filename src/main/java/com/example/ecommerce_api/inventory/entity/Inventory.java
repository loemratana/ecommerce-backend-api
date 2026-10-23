package com.example.ecommerce_api.inventory.entity;


import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.InsufficientStockException;
import com.example.ecommerce_api.product.entity.ProductVariants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stock record for a single product variant.
 *
 * quantity         = units physically on hand
 * reservedQuantity = units held for orders that are not paid yet
 * available        = quantity - reservedQuantity (what can still be sold)
 *
 * All stock changes should go through the domain methods below so the invariant
 * 0 <= reservedQuantity <= quantity always holds.
 */
@Entity
@Table(
        name = "inventories",
        //Each product variant has exactly one inventory record.
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_product_variant",
                        columnNames = "product_variant_id"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity = 0;

    @Min(0)
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_variant_id",
            nullable = false,
            unique = true
    )
    private ProductVariants productVariant;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Inventory(ProductVariants productVariant, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new BadRequestException("Initial quantity cannot be negative");
        }
        this.productVariant = productVariant;
        this.quantity = initialQuantity;
        this.reservedQuantity = 0;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean canFulfill(int requested) {
        return requested > 0 && getAvailableQuantity() >= requested;
    }

    // ---------------------------------------------------------------------
    // Domain operations
    // ---------------------------------------------------------------------

    /** Adds units to on-hand stock (supplier delivery, customer return). */
    public void increase(int amount) {
        requirePositive(amount);
        quantity += amount;
    }

    /**
     * Removes units from on-hand stock (damage, loss, manual correction).
     * Reserved units cannot be removed, otherwise pending orders could not be fulfilled.
     */
    public void decrease(int amount) {
        requirePositive(amount);
        if (amount > getAvailableQuantity()) {
            throw new InsufficientStockException(
                    "Cannot remove " + amount + " units, only " + getAvailableQuantity() + " available"
            );
        }
        quantity -= amount;
    }

    /** Holds units for an order that is waiting for payment. */
    public void reserve(int amount) {
        requirePositive(amount);
        if (!canFulfill(amount)) {
            throw new InsufficientStockException(
                    "Insufficient stock: requested " + amount + ", available " + getAvailableQuantity()
            );
        }
        reservedQuantity += amount;
    }

    /** Gives reserved units back to available stock (order cancelled / payment failed). */
    public void release(int amount) {
        requirePositive(amount);
        if (amount > reservedQuantity) {
            throw new BadRequestException(
                    "Cannot release " + amount + " units, only " + reservedQuantity + " reserved"
            );
        }
        reservedQuantity -= amount;
    }

    /** Turns a reservation into a sale: the units leave the warehouse. */
    public void confirmSale(int amount) {
        requirePositive(amount);
        if (amount > reservedQuantity) {
            throw new BadRequestException(
                    "Cannot sell " + amount + " units, only " + reservedQuantity + " reserved"
            );
        }
        reservedQuantity -= amount;
        quantity -= amount;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
    }
}
