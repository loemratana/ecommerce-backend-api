package com.example.ecommerce_api.product.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_images",
        indexes = {
                @Index(name = "idx_product_image_product", columnList = "product_id")
        }
)
public class ProductImage {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    /** Variant color this image shows; null = general image shown for every color. */
    @Column(length = 50)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
