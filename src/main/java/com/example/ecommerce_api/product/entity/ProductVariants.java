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

    @Column(precision = 19,scale = 2,nullable = false)
    private BigDecimal price;

    @Column(name = "discount_percent", precision = 5 , scale = 2)
    private BigDecimal discountPercent;

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
     * Discount actually applied to this variant: its own discountPercent if set (> 0),
     * otherwise the product-wide discountPercent, otherwise null. Capped at 100.
     */
    @Transient
    public BigDecimal getEffectiveDiscountPercent()
    {
        BigDecimal pct = positiveOrNull(discountPercent);
        if (pct == null && product != null)
        {
            pct = positiveOrNull(product.getDiscountPercent());
        }
        return pct == null ? null : pct.min(BigDecimal.valueOf(100));
    }

    @Transient
    public BigDecimal getPriceAfterDiscount()
    {
        if (price ==null)
        {
            return BigDecimal.ZERO;
        }

        BigDecimal pct = getEffectiveDiscountPercent();
        if (pct == null)
        {
            return price;
        }

        BigDecimal discountAmount = price.multiply(pct).divide(BigDecimal.valueOf(100),2, RoundingMode.HALF_UP);

        return price.subtract(discountAmount).setScale(2,RoundingMode.HALF_UP);
    }

    private static BigDecimal positiveOrNull(BigDecimal value)
    {
        return value == null || value.signum() <= 0 ? null : value;
    }


    @Transient
    public BigDecimal getDiscountAmount()
    {
        if (price  ==null)
        {
            return BigDecimal.ZERO;
        }
        return price.subtract(getPriceAfterDiscount());
    }
    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }
}
