package com.example.ecommerce_api.product.productSpecification;

import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> filter(String title,
                                                Long brandId,
                                                Long subCategoryId,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice,
                                                BigDecimal minRating) {
        List<Specification<Product>> specifications = Stream.of(
                titleContains(title),
                hasBrand(brandId),
                hasSubCategory(subCategoryId),
                priceGreaterThanOrEqualTo(minPrice),
                priceLessThanOrEqualTo(maxPrice),
                ratingGreaterThanOrEqualTo(minRating)
        ).filter(Objects::nonNull).toList();

        return specifications.isEmpty() ? Specification.unrestricted() : Specification.allOf(specifications);
    }

    public static Specification<Product> titleContains(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String search = "%" + title.trim().toLowerCase() + "%";

        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), search);

    }

    public static Specification<Product> hasBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> hasSubCategory(Long subCategoryId) {
        if (subCategoryId == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.equal(root.get("subCategory").get("id"), subCategoryId);
    }

    public static Specification<Product> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Product, ProductVariants> variants = root.join("variants", JoinType.INNER);

            return cb.greaterThanOrEqualTo(variants.get("price"), minPrice);
        };
    }

    public static Specification<Product> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }

        return ((root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Product, ProductVariants> variants = root.join("variants", JoinType.INNER);
            return criteriaBuilder.lessThanOrEqualTo(variants.get("price"), maxPrice);
        });
    }

    private static Specification<Product> ratingGreaterThanOrEqualTo(
            BigDecimal minRating
    ) {
        if (minRating == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }
}
