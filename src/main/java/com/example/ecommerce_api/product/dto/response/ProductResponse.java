package com.example.ecommerce_api.product.dto.response;


import com.example.ecommerce_api.Enum.CatalogStatus;
import com.example.ecommerce_api.Enum.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private CatalogStatus status;

    private ProductType productType;

    private String title;

    private String slug;

    private String description;

    private String imageCover;

    /** Product-wide discount (variants may override it with their own). */
    private BigDecimal discountPercent;

    private BigDecimal ratingsAverage;

    private Integer ratingsQuantity;

    private SubCategoryResponse subCategory;


   private BrandResponse brand;

    private List<ProductVariantResponse> variants;

    private List<ProductImageResponse> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
