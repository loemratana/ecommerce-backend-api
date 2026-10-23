package com.example.ecommerce_api.order.entity;


import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.product.entity.ProductVariants;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line of an order. Product name, SKU, color, size and prices are copied from the
 * variant when the order is placed, so the order keeps showing what was actually bought
 * even after the product is edited or the variant is deleted (productVariant becomes null).
 */
@Getter
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order_id", columnList = "order_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;


    /** Variant's regular unit price at order time. */
    @NotNull
    @DecimalMin(value = "0.01")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;


    /** Unit price actually charged (variant sale price, if any). Coupons are applied on the order. */
    @DecimalMin(value = "0.00")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal priceAfterDiscount;


    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariants productVariant;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(length = 50, nullable = false)
    private String sku;

    @Column(length = 50)
    private String color;

    /** Image of the ordered color at order time. */
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "size_name", length = 50)
    private String sizeName;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderItem(ProductVariants variant, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        this.productVariant = variant;
        this.quantity = quantity;
        this.price = variant.getPrice();
        this.priceAfterDiscount = variant.getPriceAfterDiscount() != null
                ? variant.getPriceAfterDiscount()
                : variant.getPrice();
        this.productName = variant.getProduct() != null ? variant.getProduct().getTitle() : variant.getSku();
        this.sku = variant.getSku();
        this.color = variant.getColor();
        this.sizeName = variant.getSize() != null ? variant.getSize().getName() : null;
        this.imageUrl = variant.getProduct() != null ? variant.getProduct().imageFor(variant.getColor()) : null;
    }

    public BigDecimal getLineTotal() {
        return priceAfterDiscount.multiply(BigDecimal.valueOf(quantity));
    }
}
