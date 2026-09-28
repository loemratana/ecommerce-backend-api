package com.example.ecommerce_api.product.dto.response;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {

    private Long id;

    private String title;

    private String slug;

    private String description;

    private String imageCover;

    private BigDecimal ratingsAverage;

    private Integer ratingsQuantity;

    private SubCategoryResponse subCategory;


   private BrandResponse brand;

    private List<ProductVariantResponse> variants;

    private List<ProductImageResponse> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
