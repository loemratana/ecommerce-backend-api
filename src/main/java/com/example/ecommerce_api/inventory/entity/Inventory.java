package com.example.ecommerce_api.inventory.entity;


import com.example.ecommerce_api.product.entity.ProductVariants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
