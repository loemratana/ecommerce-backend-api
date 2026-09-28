package com.example.ecommerce_api.category.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String image;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
