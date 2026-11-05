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
import java.math.RoundingMode;
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

    /** Regular unit price. Discounts are set on the Product only (Product.discountPercent). */
    @Column(precision = 19,scale = 2,nullable = false)
    private BigDecimal price;

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



    /**
     * The product's discount (0-100] that applies to this variant, or null when the product has none.
     * Variants have no discount of their own.
     */
    @Transient
    public BigDecimal getEffectiveDiscountPercent()
    {
        BigDecimal pct = product == null ? null : product.getDiscountPercent();
        if (pct == null || pct.signum() <= 0)
        {
            return null;
        }
        return pct.min(BigDecimal.valueOf(100));
    }

    /** Unit price the customer pays: price minus the product discount, rounded to cents. */
    @Transient
    public BigDecimal getPriceAfterDiscount()
    {
        if (price == null)
        {
            return BigDecimal.ZERO;
        }
        return price.subtract(getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Amount taken off one unit by the product discount (0 when there is none). */
    @Transient
    public BigDecimal getDiscountAmount()
    {
        BigDecimal pct = getEffectiveDiscountPercent();
        if (price == null || pct == null)
        {
            return BigDecimal.ZERO.setScale(2);
        }
        return price.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }
}
