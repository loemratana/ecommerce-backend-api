package com.example.ecommerce_api.category.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CategoryDetailResponse {

    private Long id;
    private String name;
    private String slug;
    private String image;

    private List<SubCategoryDetailResponse> subCategories;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
