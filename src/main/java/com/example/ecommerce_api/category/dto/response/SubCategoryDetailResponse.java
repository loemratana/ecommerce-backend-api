package com.example.ecommerce_api.category.dto.response;

import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubCategoryDetailResponse {



    private Long id;
    private String name;
    private String slug;
    private CategorySummaryResponse category;


    private List<ProductSummaryResponse> products;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
