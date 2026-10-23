package com.example.ecommerce_api.product.entity;


import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.brand.entity.Brand;
import com.example.ecommerce_api.category.entity.SubCategory;
import com.example.ecommerce_api.review.entity.Review;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_product_sub_category", columnList = "sub_category_id"),
                @Index(name = "idx_product_brand", columnList = "brand_id")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_cover", length = 2048)
    private String imageCover;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @Column(name = "ratings_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingsAverage = BigDecimal.ZERO;

    @Column(name = "ratings_quantity", nullable = false)
    private int ratingsQuantity = 0;




    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<ProductVariants> variants = new ArrayList<>();


    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<ProductImage> images = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;



    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<Review> reviews = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    /**
     * Best image to show for a chosen variant color: the first image tagged with that
     * color, else the cover, else the first general image.
     */
    public String imageFor(String color) {
        Comparator<ProductImage> bySortOrder = Comparator.comparingInt(ProductImage::getSortOrder);
        if (color != null && images != null) {
            String wanted = color.trim();
            String match = images.stream()
                    .filter(image -> image.getColor() != null && image.getColor().trim().equalsIgnoreCase(wanted))
                    .min(bySortOrder)
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
            if (match != null) return match;
        }
        if (imageCover != null) return imageCover;
        return images == null ? null : images.stream()
                .filter(image -> image.getColor() == null)
                .min(bySortOrder)
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }
}
