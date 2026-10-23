package com.example.ecommerce_api.product.entity;


import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.inventory.entity.Inventory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariants {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(length = 50)
    private String color;

    @Column(length = 50,nullable = false,unique = true)
    private String sku;

    @Column(precision = 19,scale = 2,nullable = false)
    private BigDecimal price;

    private  BigDecimal priceAfterDiscount;

    private  Integer soldQuantity;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prouduct_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize size;


    @OneToOne(mappedBy = "productVariant",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    private Inventory inventory;


    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDate createAt;

    private LocalDateTime updateAt;


    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }
}
