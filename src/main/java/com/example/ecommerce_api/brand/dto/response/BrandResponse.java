package com.example.ecommerce_api.brand.dto.response;


import com.example.ecommerce_api.product.dto.response.ProductSummaryResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BrandResponse {

    private Long id;
    private String name;
    private String slug;
    private String image;

    List<ProductSummaryResponse> products;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
