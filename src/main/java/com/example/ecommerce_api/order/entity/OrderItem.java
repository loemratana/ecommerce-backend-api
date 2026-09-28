package com.example.ecommerce_api.order.entity;


import com.example.ecommerce_api.product.entity.ProductVariants;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;


    @NotNull
    @DecimalMin(value = "0.01")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;


    @DecimalMin(value = "0.00")
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal priceAfterDiscount;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariants productVariant;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
